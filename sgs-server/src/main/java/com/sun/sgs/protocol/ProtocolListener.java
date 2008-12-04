/*
 * Copyright 2007-2008 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.protocol;

import com.sun.sgs.auth.Identity;
import java.math.BigInteger;

/**
 * The listener for incoming protocol connections.
 */
public interface ProtocolListener {

    /**
     * Notifies this listener that an incoming session {@code protocol} for
     * the specified {@code identity} as been established, and returns a
     * handler for processing incoming messages received by the protocol.
     *
     * @param	identity an identity
     * @param	protocol a session protocol
     * @return	a protocol handler for processing incoming messages
     */
    SessionProtocolHandler newProtocol(
	Identity identity, SessionProtocol protocol);
    
    /**
     * Notifies this listener that an incoming channel {@code protocol} for
     * the specified {@code sessionId} as been established, and returns a
     * handler for processing incoming messages received by the protocol.
     *
     * @param	sessionId a session ID
     * @param	protocol a channel protocol
     * @return	a protocol handler for processing incoming messages
     */
    ChannelProtocolHandler newProtocol(
	BigInteger sessionId, ChannelProtocol protocol);
					

    
}
