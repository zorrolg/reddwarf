package com.sun.sgs.nio.channels;

import java.net.NetworkInterface;

/**
 * Defines the standard socket options.
 * <p>
 * A network channel supports a subset of the socket options defined by
 * this enum and may support additional implementation specific socket
 * options. 
 */
public enum StandardSocketOption
    implements SocketOption
{

    /**
     * Allow transmission of broadcast datagrams.
     * <p>
     * The value of this socket option is a Boolean that represents whether
     * the option is enabled or disabled. The option is specific to
     * datagram-oriented channels sending IPv4 datagrams. When the socket
     * option is enabled then the channel can be used to send broadcast
     * datagrams.
     * <p>
     * The initial value of this socket option is false. The socket option
     * may be enabled or disabled at any time. Some operating systems may
     * require that the Java virtual machine be started with implementation
     * specific privileges to enable this option or send broadcast
     * datagrams.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc919.txt">RFC 929:
     *      Broadcasting Internet Datagrams</a>
     */
    SO_BROADCAST(Boolean.class),

    /**
     * Keep connection alive.
     * <p>
     * The value of this socket option is a Boolean that represents whether
     * the option is enabled or disabled. When the SO_KEEPALIVE option is
     * enabled the operating system may use a keep-alive mechanism to
     * periodically probe the other end of a connection when the connection
     * is otherwise idle. The exact semantics of the keep alive mechanism
     * are implementation specific, as is whether an implementation supports
     * such a mechanism.
     * <p>
     * The initial value of this socket option is false. The socket option
     * may be enabled or disabled at any time.
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC 1122
     *      Requirements for Internet Hosts -- Communication Layers</a>
     */
    SO_KEEPALIVE(Boolean.class),

    /**
     * The size of the socket send buffer.
     * <p>
     * The value of this socket option is an Integer that is the size of the
     * socket send buffer in bytes. The socket send buffer is an output
     * buffer used by the networking implementation. It may need to be
     * increased for high-volume connections. The value of the socket option
     * is a hint to the implementation to size the buffer and the actual
     * size may differ. The socket option can be queried to retrieve the
     * actual size.
     * <p>
     * For datagram-oriented channels, the size of the send buffer may limit
     * the size of the datagrams sent by the channel. Whether datagrams
     * larger than the buffer size are sent or discarded is implementation
     * specific.
     * <p>
     * The initial or default size of the socket send buffer is highly
     * implementation specific as is the range of allowable values. Invoking
     * the setOption method to set the socket send buffer to larger than its
     * maximum size causes it to be set to its maximum size. Whether the
     * socket send buffer can be set to a size of zero is implementation
     * specific.
     * <p>
     * This socket option may be used to change the size prior to connecting
     * or binding the socket. It is implementation specific whther the
     * option can be used to change the size of the socket send buffer after
     * the socket is bound.
     */
    SO_SNDBUF(Integer.class),

    /**
     * The size of the socket receive buffer.
     * <p>
     * The value of this socket option is an Integer that is the size of the
     * socket receive buffer in bytes. The socket receive buffer is an input
     * buffer used by the networking implementation. It may need to be
     * increased for high-volume connections or decreased to limit the
     * possible backlog of incoming data. The value of the socket option is
     * a hint to the implementation to size the buffer and the actual size
     * may differ.
     * <p>
     * For datagram-oriented channels, the size of the receive buffer may
     * limit the size of the datagrams that can be received by the channel.
     * Whether datagrams larger than the buffer size can be received is
     * implementation specific. Increasing the socket receive buffer may be
     * important for cases where datagrams arrive in bursts faster than they
     * can be processed.
     * <p>
     * In the case of stream-oriented channels and the TCP/IP protocol, the
     * size of the socket receive buffer may be used when advertising the
     * size of the TCP receive window to the remote peer.
     * <p>
     * The initial or default size of the socket receive buffer is highly
     * implementation specific as is the range of allowable values. Invoking
     * the setOption method to set the socket receive buffer to larger than
     * its maximum size causes it to be set to its maximum size. Whether the
     * socket receive buffer can be set to a size of zero is implementation
     * specific.
     * <p>
     * This socket option may be used to change the size prior to connecting
     * or binding the socket. It is implementation specific whether the
     * option can be used to change the size of the socket receive buffer
     * after the socket is bound.
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc1323.txt">RFC 1323: TCP
     *      Extensions for High Performance</a>
     */
    SO_RCVBUF(Integer.class),

    /**
     * Re-use address.
     * <p>
     * The value of this socket option is a Boolean that represents whether
     * the option is enabled or disabled. The exact semantics of this socket
     * option are socket type and implementation specific.
     * <p>
     * In the case of stream-oriented channels, this socket option will
     * usually determine whether the socket can be bound to a socket address
     * when a previous connection involving that socket address is in the
     * TIME_WAIT state. On implementations where the semantics differ, and
     * the socket option is not required to be enabled in order to bind the
     * socket when a previous connection is in this state, then the
     * implementation may choose to ignore this option.
     * <p>
     * For datagram-oriented sockets the socket option is used to allow
     * multiple programs bind to the same address. This option should be
     * enabled when the channel is to be used for Internet Protocol (IP)
     * multicasting.
     * <p>
     * The SO_REUSEADDR socket option must be configured prior to connecting
     * or binding the channel's socket. The initial value of this socket
     * option is implementation specific.
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc793.txt">RFC 793:
     *      Transmission Control Protocol</a>
     */
    SO_REUSEADDR(Boolean.class),

    /**
     * Linger on close if data is present.
     * <p>
     * The value of this socket option is an Integer that controls the
     * action taken when unsent data is queued on the channel socket and the
     * channel's close method is invoked to close the channel. If the value
     * of the socket option is zero or greater, then it represents a timeout
     * value, in seconds, known as the linger interval. The linger interval
     * is the timeout for the close method to block while the operating
     * system attempts to transmit the unsent data or it decides that it is
     * unable to transmit the data. If the value of the socket option is
     * less than zero then the option is disabled. In that case the close
     * method does not wait until unsent data is transmitted; if possible
     * the operating system will transmit any unsent data before the
     * connection is closed.
     * <p>
     * This socket option is intended for use with channels that are
     * configured in blocking mode only. The behavior of the close method
     * when this option is enabled on a non-blocking channel is not defined.
     * <p>
     * The initial value of this socket option is a negative value, meaning
     * that the option is disabled. The option may be enabled, or the linger
     * interval changed, at any time. The maximum value of the linger
     * interval is implementation specific. Invoking the setOption method to
     * set the linger interval to a value that is greater than its maximum
     * value causes the linger interval to be set to its maximum value.
     */
    SO_LINGER(Integer.class),

    /**
     * The Type of Service (ToS) octet in the Internet Protocol (IP) header.
     * <p>
     * The value of this socket option is an Integer, the least significant
     * 8 bits of which represents the value of the ToS octet in IP packets
     * sent by channels to an IPv4 socket. The interpretation of the ToS
     * octet is network specific and is not defined by this class. Further
     * information on the ToS octet can be found in RFC 1349 and RFC 2474.
     * The value of the socket option that is specified to the setOption
     * method is a hint. An implementation may ignore the value, or ignore
     * specific values.
     * <p>
     * The initial or default value of the TOS field in the ToS octet is
     * implementation specific but will usually be zero. For
     * datagram-oriented channels the option may be configured at any time
     * after the socket has been bound. The new value of the octet is used
     * when sending subsequent datagrams. It is implementation specific
     * whether this option can be queried or changed prior to binding the
     * socket.
     * <p>
     * The behavior of this socket option on a stream-oriented channel, or a
     * channel to an IPv6 socket, is not defined in this release.
     */
    IP_TOS(Integer.class),

    /**
     * The network interface for Internet Protocol (IP) multicast datagrams.
     * <p>
     * The value of this socket option is a NetworkInterface that represents
     * the outgoing interface for multicast datagrams sent by the
     * datagram-oriented channel. If the channel is to an IPv6 socket then
     * it is implementation specific whether the option also applies to
     * multlicast datagrams sent to IPv4-mapped IPv6 addresses.
     * <p>
     * The initial value of this socket option is implementation specific
     * and invoking getOption to query this socket option may return null. A
     * value of null implies that the outgoing interface will be selected by
     * the operating system, typically based on the network routing tables.
     * <p>
     * The network interface for outgoing multicast datagrams can be set
     * after the socket is bound. It is implementation specific whether this
     * option can be queried or changed prior to binding the socket.
     * 
     * @see MulticastChannel
     */
    IP_MULTICAST_IF(NetworkInterface.class),

    /**
     * The time-to-live for Internet Protocol (IP) multicast datagrams.
     * <p>
     * The value of this socket option is an {@code Integer} in the range
     * {@code 0 <= value <= 255}. It is used to control the scope of
     * multicast datagrams sent by the datagram-oriented channel. In the
     * case of an IPv4 socket the option is the time-to-live (TTL) on
     * multicast datagrams sent by the socket. Datagrams with a TTL of zero
     * are not transmitted on the network but may be delivered locally. In
     * the case of an IPv6 socket the option is the hop limit which is
     * number of hops that the datagram can pass through before expiring on
     * the network.
     * <p>
     * If the channel is to an IPv6 socket then it is implementation
     * specific whether the option sets the time-to-live on multicast
     * datagrams sent to IPv4-mapped IPv6 addresses.
     * <p>
     * The initial value of the time-to-live setting is implementation
     * specific but is typically 1. It is also implementation specific
     * whether this option can be queried or changed prior to binding the
     * socket.
     * 
     * @see MulticastChannel
     */
    IP_MULTICAST_TTL(Integer.class),

    /**
     * Loopback for Internet Protocol (IP) multicast datagrams.
     * <p>
     * The value of this socket option is a Boolean that controls the
     * loopback of multicast datagrams. The value of the socket option
     * represents if the option is enabled or disabled.
     * <p>
     * The exact semantics of this socket options are implementation
     * specific. In particular, it is implementation specific whether the
     * socket option applies to multicast datagrams sent from the socket or
     * received by the socket.
     * <p>
     * If the channel is to an IPv6 socket then it is implementation
     * specific whether the option applies to multicast datagrams sent to
     * IPv4-mapped IPv6 addresses.
     * <p>
     * The initial value of this socket option is implementation specific
     * but is typically true. It is also implementation specific whether
     * this option can be queried or changed prior to binding the socket.
     * 
     * @see MulticastChannel
     */
    IP_MULTICAST_LOOP(Boolean.class),

    /**
     * Disable the Nagle algorithm.
     * <p>
     * The value of this socket option is a Boolean that represents whether
     * the option is enabled or disabled. The socket option is specific to
     * stream-oriented channels using the TCP/IP protocol. TCP/IP uses an
     * algorithm known as The Nagle Algorithm to coalesce short segments and
     * improve network efficiency.
     * <p>
     * The default value of this socket option is false. The socket option
     * should only be enabled in cases where it is known that the coalescing
     * impacts performance. The socket option may be enabled at any time. In
     * other words, the Nagle Algorithm can be disabled. Once the option is
     * enabled, it is implementation specific whether it can be subsequently
     * disabled. In that case, invoking the setOption method to disable the
     * option has no effect.
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC 1122:
     *      Requirements for Internet Hosts -- Communication Layers</a>
     */
    TCP_NODELAY(Boolean.class);


    private final Class<?> type;

    private StandardSocketOption(Class<?> type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public final Class<?> type() {
        return type;
    }
}
