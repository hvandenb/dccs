package org.mines.cs565.dccs.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import io.atomix.Atomix;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.collections.DistributedQueue;
import io.atomix.coordination.DistributedLeaderElection;
import io.atomix.coordination.DistributedMembershipGroup;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import lombok.extern.slf4j.Slf4j;
import scala.annotation.meta.getter;

import org.slf4j.Logger;

@Service
@Slf4j
public class ClusterManager {

	@Autowired
	private ClusterProperties settings;
	
	private static Atomix atomix = null;

	List<Address> members = new ArrayList<Address>();
	private Splitter splitter;
	
	/**
	 * Create a distributed queue
	 * @param queueName name of the queue, if the queueName is empty or null a null will returned
	 * @return a CompletableFuture queue
	 */
	public static CompletableFuture<DistributedQueue<String>> createQueue(String queueName) {
		
		if (Strings.nullToEmpty(queueName).isEmpty() || atomix == null)
			return null;
		
		CompletableFuture<DistributedQueue<String>> queue = atomix.<DistributedQueue<String>>create(queueName, DistributedQueue.class);
		
		return queue;
		
	}

	/**
	 * Run the Cluster an we get invoked in an Async way by the task scheduler so we can keep
	 * running
	 */
	@Async
	private void run() {
		atomix.open().join();

		log.info("Creating membership group");
		DistributedMembershipGroup group;
		try {
			group = atomix.create(settings.getName(), DistributedMembershipGroup.class).get();

			log.info("Joining the {} group", settings.getName());
			group.join().thenAccept(member -> {
				log.info("Joined group with member ID: " + member.id());
			});

			group.onJoin(member -> {
				log.info(member.id() + " joined the group");
			});

			group.onLeave(member -> {
				log.info(member.id() + " left the group");
			});

		} catch (InterruptedException | ExecutionException e1) {
			log.error("Unable to join a group: [{}]", e1);
		}

		// Create a leader election resource.
		DistributedLeaderElection election;
		try {
			election = atomix.create("election", DistributedLeaderElection.class).get();

			// Register a callback to be called when this election instance is
			// elected the leader
			election.onElection(epoch -> {

				log.info("We've been elected as the leader");

				// Verify that this node is still the leader
				election.isLeader(epoch).thenAccept(leader -> {
					if (leader) {
						log.info("We're still the leader");
						// Do something important
					} else {
						log.info("Lost leadership!");
					}
				});

			}).join();

		} catch (InterruptedException | ExecutionException e) {
			log.error("Election was interupted due to [{}]", e.getMessage());
		}

	    while (atomix.isOpen()) {
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
		
//		atomix.open().thenRun(() -> {
//
//			log.info("Replica started!");
//		});

	}
	
//	@PostConstruct
	void init() {

		log.info("Initialize the ClusterManager");
		this.splitter = Splitter.onPattern(ClusterConstants.DEFAULT_DELIMITER).omitEmptyStrings().trimResults();

        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Address address = new Address(settings.getHostName(), settings.getPort());

		buildPeers(this.settings.getMembers());

		String logDir = settings.getLogLocation() + UUID.randomUUID().toString();
		log.info("Data Log location: [{}]", logDir);
		atomix = AtomixReplica.builder(address, this.members).
				withTransport(new NettyTransport()).
				withStorage(Storage.builder().withDirectory(logDir).build()).
				build();
		
		this.run(); // Async call
	}

	/**
	 * Build the server peers from a list of comma separated list of peers.
	 * 
	 * @param peers
	 */
	private void buildPeers(String peers) {
		log.info("Building Members: " + peers);
		List<String> peerList = Lists.newArrayList(this.splitter.split(Strings.nullToEmpty(peers)));

		this.members.clear();

		for (String s : peerList) {
			HostAndPort hp = HostAndPort.fromString(s).withDefaultPort(ClusterConstants.DEFAULT_PORT)
					.requireBracketsForIPv6();
			this.members.add(new Address(hp.getHostText(), hp.getPort()));
		}
	}

}
