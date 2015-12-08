package org.mines.cs565.dccs.cluster;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
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

import io.atomix.AtomixReplica;
import io.atomix.AtomixReplica.Builder;
import io.atomix.atomic.DistributedAtomicValue;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.collections.DistributedQueue;
import io.atomix.coordination.DistributedLeaderElection;
import io.atomix.coordination.DistributedMembershipGroup;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
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

	private Splitter splitter;

	private static Optional<AtomixReplica> server = Optional.absent();
	
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

//		if (Strings.nullToEmpty(queueName).isEmpty() || !server.isPresent())
//			return null;
//
//		CompletableFuture<DistributedQueue<String>> queue = server.get().create(queueName, DistributedQueue.class);

		return null;

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
	 * Return the current IP of the local machine
	 * 
	 * @return
	 */
	public String getCurrentIp() {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
				Enumeration<InetAddress> nias = ni.getInetAddresses();
				while (nias.hasMoreElements()) {
					InetAddress ia = (InetAddress) nias.nextElement();
					if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia instanceof Inet4Address) {
						return ia.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			log.error("unable to get current IP " + e.getMessage(), e);
		}
		return "";
	}

	/**
	 * Initializes the Cluster Manager and sets up the cluster
	 */
	@PostConstruct
	void init() {

		log.info("Initialize the ClusterManager");
		this.splitter = Splitter.onPattern(ClusterConstants.DEFAULT_DELIMITER).omitEmptyStrings().trimResults();

		// Get some sleep we'll be needing it
		sleep(1000);

		gossiper.startAsync();

		try {
			gossiper.awaitRunning(2, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Timed out on waiting for gossiper to start");
		}

		log.info("We have {} members that are live", gossiper.members().size());

		cluster.startAsync();
		try {
			cluster.awaitRunning(2, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Timed out on waiting for cluster to start");
		}

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

		List<GossipService> clients = new ArrayList<>();
		List<GossipMember> seedMembers = new ArrayList<GossipMember>();

		/**
		 * Retrieve the list of LIVE members
		 * 
		 * @return List of Live Members {@link LocalGossipMember}
		 */
		public List<LocalGossipMember> members() {

			//TODO Remove our self from the list
			
			Set<LocalGossipMember> l = new HashSet<LocalGossipMember>();
			
			int i = 0;
			for (GossipService c : clients) {
				l.addAll(clients.get(i).get_gossipManager().getMemberList());
				l.remove(clients.get(i).get_gossipManager().getMyself()); // We'll remove ourselves from the list
				i++;
			}

			return new ArrayList<LocalGossipMember>(l);

		}

		@Override
		protected void run() throws Exception {

			// Start the gossiping...
			start();

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
		private void start() {

			GossipSettings gossipSettings = new GossipSettings(settings.getGossipInterval(),
					settings.getGossipCleanupInterval());

			List<HostAndPort> seeds = buildEndpointList(settings.getSeeds(), settings.getGossipPort());
			seedMembers = Lists.newArrayListWithCapacity(seeds.size());
			seedMembers.clear();

			for (HostAndPort hap : seeds) {
				seedMembers.add(new RemoteGossipMember(hap.getHostText(), hap.getPort(),
						Node.generateId(hap.getHostText(), hap.getPort()), settings.getHeartBeat()));
			}

			log.info("Initializing Gossip, with seeds {} ", seeds);

			String myIpAddress = Strings.nullToEmpty(getCurrentIp());
			log.info("Using [{}] for our gossip address", myIpAddress);

			// Lets start the gossip clients.
			// Start the clients, waiting cleaning-interval + 1 second between
			// them which will show the
			// dead list handling.
			for (GossipMember member : seedMembers) {

				GossipService gossipService;
				try {
					gossipService = new GossipService(myIpAddress, member.getPort(),
							Node.generateId(member.getHost(), member.getPort()), LogLevel.DEBUG,
							(ArrayList<GossipMember>) seedMembers, gossipSettings, this);

					clients.add(gossipService);

					gossipService.start();
					log.info("Started Gossip service {} on Thread [{}]", gossipService, gossipService.get_gossipManager().getName());
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
	private class Cluster extends AbstractExecutionThreadService implements Consumer<Long> {

		List<Address> members = new ArrayList<Address>();
		Optional<DistributedMembershipGroup> group;
		DistributedLeaderElection election; 			// Create a leader election resource.
		AtomicBoolean isLeader = new AtomicBoolean(Boolean.FALSE);
		DistributedAtomicValue<List<Boolean>> value;
		
		/**
		 * Convert a list of Gossip Members to RAFT Cluster Members
		 * 
		 * @param list of gossip members
		 */
		private List<Address> convertToMembers(List<LocalGossipMember> l) {
			List<Address> list = Lists.newArrayListWithCapacity(l.size());
			
			log.info("Building Members: {}", l);
			
			list.clear();

			for (LocalGossipMember m : l) {
				list.add(new Address(m.getHost(), settings.getPort()));
			}
			
			return list;
		}

		private DistributedAtomicValue<List<Boolean>> createValue(String name) {
			DistributedAtomicValue<List<Boolean>> r = null;
			log.info("Initializing value {}", name);
			try {
				r = server.get().<DistributedAtomicValue<List<Boolean>>>create(name, DistributedAtomicValue.class).get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Unable to create the value {}, due to {}", name, e.getMessage());
			}
			
//			.thenAccept(value -> {
				log.info("Value created");
//			});
			
			return r;
		}
		
		/**
		 * Starts the Leader Election process.
		 */
		private void election() {

			log.info("Starting the Leader Election process");

			try {
				// Create a leader election resource.
				election = server.get().create("election", DistributedLeaderElection.class).get(2, TimeUnit.SECONDS);

				// Register a callback to be called when this election instance
				// is elected the leader
				election.onElection(this).join();

			} catch (InterruptedException | ExecutionException e) {
				log.error("Election was interupted due to [{}]", e.getMessage());
			} catch (TimeoutException e) {
				log.error("Election creation timed out [{}]}", e.getLocalizedMessage());
			}
		}

		/**
		 * Join a named group
		 * 
		 * @param name
		 */
		private void join(String name) {
			try {

				log.info("Creating {} membership group", name);

				group = Optional.of(server.get().create(name, DistributedMembershipGroup.class).get());

				log.info("Joining the {} group", name);
				group.get().join().thenAccept(member -> {
					log.info("Joined group with member ID: " + member.id());
				});

				group.get().onJoin(member -> {
					log.info(member.id() + " joined the group");
				});

				group.get().onLeave(member -> {
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
			List<LocalGossipMember> l = null;
			int quorum = 0;
			
			// start the server using sync calls
			log.info("Starting the RAFT Cluster");
			
			l = gossiper.members();

			// We don't start the server till we have larger size
			while(quorum < settings.getMinimumQuorum()) {
				// Let's make sure there is no request for shutdown
				if (!isRunning())
					return;
				
				l.clear();
				l = gossiper.members();
				quorum = (l.size()/2)+1;
				log.info("Quorum of [{}] not yet met, quorum: [{}] n: [{}]", settings.getMinimumQuorum(), quorum, l.size());
				
				
				sleep(1000);			
			}
			
			log.info("Quorum met time to start the server");
			
			start(l);
					
			// Main loop for the cluster manager
			while (isRunning()) {
				l.clear();
				l = gossiper.members();
				quorum = (l.size()/2)+1;
				log.info("live members [{}], quorum [{}] leader? [{}]", l.size(), quorum, isLeader.get());
				
				// Do an election when the service is open
				if (server.isPresent() && server.get().isOpen()){
						election();
				}

				
				sleep(settings.getElectionTimeout());
			}
		}
		
		
		/**
		 * Start up the RAFT server
		 * @param l list of found servers through gossiping..
		 */
		private void start(List<LocalGossipMember> l) {
			
			String host = getCurrentIp();
			if (Strings.isNullOrEmpty(host))
				host = settings.getHostName();
			log.info("Initialize the Cluster at [{}:{}]", host, settings.getPort());

			Address address = new Address(host,
			settings.getPort());
			
			members = convertToMembers(l);

			String logDir = settings.getLogLocation() +
			UUID.randomUUID().toString();
			log.info("Data Log location: [{}]", logDir);

			// Setup and initialize the Raft Server
			Builder builder = AtomixReplica.builder(address, members);
			
//			serverBuilder = CopycatServer.builder(address, members);
			builder.withTransport(new NettyTransport());
			builder.withElectionTimeout(Duration.ofSeconds(settings.getElectionTimeout()));
			builder.withHeartbeatInterval(Duration.ofSeconds(settings.getHeartBeat()));

			 // TODO: Need to something for storage
			 //			 Storage s = new Storage(logDir, StorageLevel.DISK);
			 Storage s = new Storage(logDir);
			 builder.withStorage(s);

			builder.withTransport(new NettyTransport());
			 
			server = Optional.of(builder.build());
			server.get().open().join();
			
//			.thenRun(() -> {
				  log.info("RAFT Server has started");
//					join(settings.getName());
					
					value = createValue("vector");

//			});			
		}


		/**
		 * This gets called when there is an election change
		 */
		@Override
		public void accept(Long t) {
				log.info("We've been elected as the leader");
				isLeader.set(Boolean.TRUE);
				// Verify that this node is still the leader
				election.isLeader(t).thenAccept(leader -> {
					if (leader) {
						log.info("We're still the leader");
						isLeader.set(Boolean.TRUE);
						
						// Do something important
					} else {
						log.info("Lost leadership!");
						isLeader.set(Boolean.FALSE);
					}
				});

			}	
		
	}

}
