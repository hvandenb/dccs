package org.mines.cs565.dccs.cluster;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

import com.google.code.gossip.GossipMember;
import com.google.code.gossip.GossipService;
import com.google.code.gossip.GossipSettings;
import com.google.code.gossip.LocalGossipMember;
import com.google.code.gossip.LogLevel;
import com.google.code.gossip.RemoteGossipMember;
import com.google.code.gossip.event.GossipListener;
import com.google.code.gossip.event.GossipState;
import com.google.common.base.Optional;
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

/**
 * 
 * @author <a href="http://github.com/hvandenb">Henri van den Bulk</a>
 *
 */
@Service
@Slf4j
public class ClusterManager {

	@Autowired
	private ClusterProperties settings;

	private final Cluster cluster;
	private final Gossiper gossiper;

	private static Optional<Atomix> server = null;

	List<Address> members = new ArrayList<Address>();
	List<GossipService> clients = new ArrayList<>();
	List<GossipMember> seedMembers = new ArrayList<GossipMember>();

	private Splitter splitter;

	/**
	 * Constructor...
	 */
	public ClusterManager() {
		this.cluster = new Cluster();
		this.gossiper = new Gossiper();
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

		if (Strings.nullToEmpty(queueName).isEmpty() || !server.isPresent())
			return null;

		CompletableFuture<DistributedQueue<String>> queue = server.get().create(queueName, DistributedQueue.class);

		return queue;

	}
	/** Simple wrapper */
	private void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			log.warn(e.getLocalizedMessage());
		}
		
	}
	
	/**
	 * Initializes the Cluster Manager and sets up the cluster
	 */
	@PostConstruct
	void init() {

		log.info("Initialize the ClusterManager at [{}:{}]", settings.getHostName(), settings.getPort());
		this.splitter = Splitter.onPattern(ClusterConstants.DEFAULT_DELIMITER).omitEmptyStrings().trimResults();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}

		gossiper.startAsync();

		try {
			gossiper.awaitRunning(2, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Timed out on waiting for gossiper to start");
		}

		
//		Address address = new Address(settings.getHostName(), settings.getPort());
//
//		buildPeers(settings.getMembers());
//
//		String logDir = settings.getLogLocation() + UUID.randomUUID().toString();
//		log.info("Data Log location: [{}]", logDir);
//
//		// Setup and initialize the Raft Server
//		Builder builder = AtomixReplica.builder(address, this.members);
//		// Storage s = new Storage(logDir, StorageLevel.DISK);
//		Storage s = new Storage(logDir);
//		builder.withStorage(s).withTransport(new NettyTransport());
//		server = Optional.of(builder.build());
//
//		cluster.startAsync();
//		try {
//			cluster.awaitRunning(2, TimeUnit.SECONDS);
//		} catch (TimeoutException e) {
//			log.warn("Timed out on waiting for cluster to start");
//		}

		// cluster.run(); // Async call
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

		members.clear();

		for (String s : peerList) {
			HostAndPort hp = HostAndPort.fromString(s).withDefaultPort(ClusterConstants.DEFAULT_PORT)
					.requireBracketsForIPv6();
			members.add(new Address(hp.getHostText(), hp.getPort()));
		}
	}

	/**
	 * Based on a string of delimiter separate host and port build a list of
	 * them
	 * 
	 * @param s
	 *            - String containing the list of host/ports
	 * @param defaultPort
	 *            - Default port to use if a port was not specified.
	 * @return
	 */
	private List<HostAndPort> buildEndpointList(String s, int defaultPort) {
		List<String> sl = Lists.newArrayList(this.splitter.split(Strings.nullToEmpty(s)));
		List<HostAndPort> hapl = Lists.newArrayListWithCapacity(sl.size());

		for (String e : sl) {
			hapl.add(HostAndPort.fromString(e).withDefaultPort(defaultPort).requireBracketsForIPv6());
		}

		return hapl;
	}

	/**
	 * 
	 * @author Henri van den Bulk
	 *
	 */
	private class Gossiper extends AbstractExecutionThreadService implements GossipListener, Closeable {

		/**
		 * Retrieve the list of LIVE members
		 * @return List of Live Members {@link LocalGossipMember}
		 */
		public List<LocalGossipMember> members() {
			getMemberList()
		}
		@Override
		protected void run() throws Exception {

			// Start the gossiping...
			startGossip();

			// We keep checking if we still should be running
			while (isRunning()) {
				sleep(1000);
			}

			// Close all the client connections
			close();
		}

		/**
		 * Start the gossip to find all the other members in the cluster. TODO:
		 * https://github.com/edwardcapriolo/gossip/tree/
		 * b18821d41e147589bf70a594bc6937666b65c406
		 */
		private void startGossip() {

			GossipSettings gossipSettings = new GossipSettings();
			List<HostAndPort> seeds = buildEndpointList(settings.getSeeds(), ClusterConstants.DEFAULT_GOSSIP_PORT);
			seedMembers = Lists.newArrayListWithCapacity(seeds.size());
			seedMembers.clear();

			for (HostAndPort hap : seeds) {
				seedMembers.add(new RemoteGossipMember(hap.getHostText(), 
						hap.getPort(),
						Node.generateId(hap.getHostText(), hap.getPort()),
						settings.getHeartBeat()));
			}

			log.info("Initializing Gossip, with seeds {} ", seeds);

			String myIpAddress = "";
			try {
				myIpAddress = InetAddress.getLocalHost().getHostAddress();
				log.info("Using [{}] for our gossip address", myIpAddress);
			} catch (UnknownHostException e1) {
				log.warn(e1.getLocalizedMessage());
			}

			// Lets start the gossip clients.
			// Start the clients, waiting cleaning-interval + 1 second between
			// them which will show the
			// dead list handling.
			for (GossipMember member : seedMembers) {

				GossipService gossipService;
				try {
					gossipService = new GossipService(myIpAddress, member.getPort(),
							Node.generateId(member.getHost(), member.getPort()), LogLevel.DEBUG, (ArrayList<GossipMember>) seedMembers, gossipSettings,this);
					
					clients.add(gossipService);
					gossipService.start();
					sleep(1000);

				} catch (UnknownHostException | InterruptedException e) {
					log.warn(e.getLocalizedMessage());
				}

			}
		}
	

		@Override
		public void gossipEvent(GossipMember member, GossipState state) {
			log.info("Gossip Event {}, state [{}]", member, state);
		}

		@Override
		public void close() throws IOException {
		    for (GossipService c : clients) {
		        c.shutdown();
		      }			
		}

	}

	/**
	 * The Cluster is an internal class that manages the cluster as a background
	 * Service.
	 * 
	 * @author Henri van den Bulk
	 *
	 */
	private class Cluster extends AbstractExecutionThreadService {

		/**
		 * Starts the Leader Election process.
		 */
		private void performLeaderElection() {

			log.info("Starting the Leader Election process");
			// Create a leader election resource.
			DistributedLeaderElection election;
			try {
				election = server.get().create("election", DistributedLeaderElection.class).get();

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
		}
		
		/**
		 * Join a named group
		 * 
		 * @param name
		 */
		private void joinGroup(String name) {
			try {

				log.info("Creating membership group");
				DistributedMembershipGroup group;

				group = server.get().create(name, DistributedMembershipGroup.class).get();

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
				log.error("Unable to join a group: [{}]", e1.getLocalizedMessage());
			}
		}

		/**
		 * Run the Cluster an we get invoked in an Async way by the task
		 * scheduler so we can keep running
		 */
		// @Async
		@Override
		protected void run() {
			// server.open().join();

			// start the server using sync calls
			log.info("Opening the Atomix Server");

			if (server.isPresent()) {

				server.get().open().join();

				log.info("Replica started!");

				performLeaderElection();

				// joinGroup(settings.getName());

				// We keep checking if we still should be running
				while (isRunning()) {
					while (server.get().isOpen()) {
						sleep(1000);
					}

				}
			} else {
				log.warn("The server was not initialized as for some reason the server was not set");
			}
		}
	}

}
