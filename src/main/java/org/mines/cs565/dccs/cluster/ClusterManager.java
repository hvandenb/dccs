package org.mines.cs565.dccs.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

import io.atomix.Atomix;
import io.atomix.AtomixReplica;
import io.atomix.AtomixReplica.Builder;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.collections.DistributedQueue;
import io.atomix.coordination.DistributedLeaderElection;
import io.atomix.coordination.DistributedMembershipGroup;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ClusterManager {

	@Autowired
	private ClusterProperties settings;
	
	private final Cluster cluster;

	private static Atomix server = null;

	List<Address> members = new ArrayList<Address>();
	private Splitter splitter;

	/**
	 * 
	 */
	public ClusterManager() {
		this.cluster = new Cluster();
	}

	/**
	 * Create a distributed queue
	 * 
	 * @param queueName
	 *            name of the queue, if the queueName is empty or null a null
	 *            will returned
	 * @return a CompletableFuture queue
	 */
	public CompletableFuture<DistributedQueue<String>> createQueue(String queueName) {

		if (Strings.nullToEmpty(queueName).isEmpty() || server == null)
			return null;

		CompletableFuture<DistributedQueue<String>> queue = server.<DistributedQueue<String>> create(queueName,
				DistributedQueue.class);

		return queue;

	}

	/**
	 * Initializes the Cluster Manager and sets up the cluster
	 */
	@PostConstruct
	void init() {

		log.info("Initialize the ClusterManager");
		this.splitter = Splitter.onPattern(ClusterConstants.DEFAULT_DELIMITER).omitEmptyStrings().trimResults();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}

		Address address = new Address(settings.getHostName(), settings.getPort());

		buildPeers(settings.getMembers());

		String logDir = settings.getLogLocation() + UUID.randomUUID().toString();
		log.info("Data Log location: [{}]", logDir);

		// Setup and initialize the Raft Server
		Builder builder = AtomixReplica.builder(address, this.members);
		Storage s = new Storage(logDir, StorageLevel.DISK);
		builder.withStorage(s).withTransport(new NettyTransport());
		server = builder.build();
		
		cluster.startAsync();
		try {
			cluster.awaitRunning(2, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Timed out on waiting for cluster to start");
		}
		
//		cluster.run(); // Async call
		log.info("Completed Initializing the ClusterManager");
	}

	/**
	 * Close things down before the object gets destroyed.
	 */
	@PreDestroy
	public void stop() {
		log.info("Stopping the Cluster Manager");
		
		cluster.stopAsync();
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

	private class Cluster extends AbstractExecutionThreadService {

		/**
		 * Run the Cluster an we get invoked in an Async way by the task
		 * scheduler so we can keep running
		 */
//		@Async
		@Override
		protected void run() {
//			server.open().join();
			
			try {
				// start the server using sync calls
				server.open().join();
				log.info("Replica started!");

				log.info("Creating membership group");
				DistributedMembershipGroup group;

				group = server.create(settings.getName(), DistributedMembershipGroup.class).get();

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
				election = server.create("election", DistributedLeaderElection.class).get();

				// Register a callback to be called when this election instance
				// is elected the leader
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

			// We keep checking if we still should be running
		     while (isRunning()) {
					while (server.isOpen()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							log.error(e.getLocalizedMessage());
						}
					}

		       }
			

			// atomix.open().thenRun(() -> {
			//
			// log.info("Replica started!");
			// });
		}
	}

}
