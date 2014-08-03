/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
 *  Copyright (C) 2010 Mina Shokry
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @version $Id: MicroeditionConnector.java 3053 2010-07-31 21:07:20Z minashokry $
 */
package com.intel.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

import com.intel.bluetooth.gcf.socket.ServerSocketConnection;
import com.intel.bluetooth.gcf.socket.SocketConnection;
import com.intel.bluetooth.obex.OBEXClientSessionImpl;
import com.intel.bluetooth.obex.OBEXConnectionParams;
import com.intel.bluetooth.obex.OBEXSessionNotifierImpl;

/**
 * 
 * Implementation of javax.microedition.io.Connector
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * <p>
 * BlueCove specific JSR-82 extension <tt>bluecovepsm</tt> enables the use of specific PSM channel in L2CAP service.
 * <tt>btl2cap://localhost;name=...;bluecovepsm=1007</tt>
 */
public abstract class MicroeditionConnector {
	/*
	 * Access mode READ. The value 1 is assigned to READ.
	 */

	public static final int READ = Connector.READ;

	/*
	 * Access mode WRITE. The value 2 is assigned to WRITE.
	 */
	public static final int WRITE = Connector.WRITE;

	/*
	 * Access mode READ_WRITE. The value 3 is assigned to READ_WRITE.
	 */
	public static final int READ_WRITE = Connector.READ_WRITE;
	private static Hashtable/* <String, String> */ suportScheme = new Hashtable();
	private static Hashtable/* <String, String> */ srvParams = new Hashtable();
	private static Hashtable/* <String, String> */ cliParams = new Hashtable();
	private static Hashtable/* <String, String> */ cliParamsL2CAP = new Hashtable();
	private static Hashtable/* <String, String> */ srvParamsL2CAP = new Hashtable();
	private static final String AUTHENTICATE = "authenticate";
	private static final String AUTHORIZE = "authorize";
	private static final String ENCRYPT = "encrypt";
	private static final String MASTER = "master";
	private static final String NAME = "name";
	private static final String RECEIVE_MTU = "receivemtu";
	private static final String TRANSMIT_MTU = "transmitmtu";
	private static final String EXT_BLUECOVE_L2CAP_PSM = "bluecovepsm";
	private static final String ANDROID = "android";

	static {
		// cliParams ::== master | encrypt | authenticate
		cliParams.put(AUTHENTICATE, AUTHENTICATE);
		cliParams.put(ENCRYPT, ENCRYPT);
		cliParams.put(MASTER, MASTER);

		// srvParams ::== name | master | encrypt | authorize | authenticate
		copyAll(srvParams, cliParams);
		srvParams.put(AUTHORIZE, AUTHORIZE);
		srvParams.put(NAME, NAME);

		copyAll(cliParamsL2CAP, cliParams);

		cliParamsL2CAP.put(RECEIVE_MTU, RECEIVE_MTU);
		cliParamsL2CAP.put(TRANSMIT_MTU, TRANSMIT_MTU);

		copyAll(srvParamsL2CAP, cliParamsL2CAP);
		srvParamsL2CAP.put(AUTHORIZE, AUTHORIZE);
		srvParamsL2CAP.put(NAME, NAME);
		srvParamsL2CAP.put(EXT_BLUECOVE_L2CAP_PSM, EXT_BLUECOVE_L2CAP_PSM);

		// "socket://" host ":" port
		// no validation for socket, since this is internal connector

		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_L2CAP, Boolean.TRUE);
		suportScheme.put("socket", Boolean.TRUE);
	}

	private MicroeditionConnector() {
	}

	static void copyAll(Hashtable dest, Hashtable src) {
		for (Enumeration en = src.keys(); en.hasMoreElements();) {
			Object key = en.nextElement();
			dest.put(key, src.get(key));
		}
	}

	static String validParamName(Hashtable map, String paramName) {
		String validName = (String) map.get(paramName.toLowerCase());
		if (validName != null) {
			return validName;
		}

		// an addition for android.
		// a workaround to legalize the non-jsr82-compliant android connection urls
		// using a "android=true" parameter at end of the connection string.
		if (ANDROID.equals(paramName)) {
			return ANDROID;
		}
		// end of addition to legalize android connection strings.

		return null;
	}

	/*
	 * Create and open a Connection. Parameters: name - The URL for the connection. Returns: A new Connection object.
	 * Throws: IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException - If the requested
	 * connection cannot be made, or the protocol type does not exist. java.io.IOException - If some other kind of I/O
	 * error occurs. SecurityException - If a requested protocol handler is not permitted.
	 */
	public static Connection open(String name) throws IOException {
		return openImpl(name, READ_WRITE, false, true);
	}

	private static Connection openImpl(String name, int mode, boolean timeouts, boolean allowServer) throws IOException {

		DebugLog.debug("connecting", name);

		/*
		 * parse URL
		 */

		String host = null;
		String portORuuid = null;

		Hashtable values = new Hashtable();

		// scheme : // host : port [;param=val]
		int schemeEnd = name.indexOf("://");
		if (schemeEnd == -1) {
			throw new ConnectionNotFoundException(name);
		}
		String scheme = name.substring(0, schemeEnd);
		if (!suportScheme.containsKey(scheme)) {
			throw new ConnectionNotFoundException(scheme);
		}
		boolean schemeBluetooth = (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM))
				|| (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX) || (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_L2CAP)));
		boolean isL2CAP = scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_L2CAP);
		boolean isTCPOBEX = scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX);

		BluetoothStack bluetoothStack = null;

		if (schemeBluetooth) {
			bluetoothStack = BlueCoveImpl.instance().getBluetoothStack();
		}

		boolean isServer;

		int hostEnd = name.indexOf(':', scheme.length() + 3);

		if (hostEnd > -1) {
			host = name.substring(scheme.length() + 3, hostEnd);
			isServer = host.equals("localhost");

			Hashtable params;
			if (isTCPOBEX) {
				params = new Hashtable();
				isServer = (host.length() == 0);
			} else if (isL2CAP) {
				if (isServer) {
					params = srvParamsL2CAP;
				} else {
					params = cliParamsL2CAP;
				}
			} else {
				if (isServer) {
					params = srvParams;
				} else {
					params = cliParams;
				}
			}

			String paramsStr = name.substring(hostEnd + 1);
			UtilsStringTokenizer tok = new UtilsStringTokenizer(paramsStr, ";");
			if (tok.hasMoreTokens()) {
				portORuuid = tok.nextToken();
			} else {
				portORuuid = paramsStr;
			}
			while (tok.hasMoreTokens()) {
				String t = tok.nextToken();
				int equals = t.indexOf('=');
				if (equals > -1) {
					String param = t.substring(0, equals);
					String value = t.substring(equals + 1);
					String validName = validParamName(params, param);
					if (validName != null) {
						String hasValue = (String) values.get(validName);
						if ((hasValue != null) && (!hasValue.equals(value))) {
							throw new IllegalArgumentException("duplicate param [" + param + "] value [" + value + "]");
						}
						values.put(validName, value);
					} else {
						throw new IllegalArgumentException("invalid param [" + param + "] value [" + value + "]");
					}
				} else {
					throw new IllegalArgumentException("invalid param [" + t + "]");
				}
			}
		} else if (isTCPOBEX) {
			host = name.substring(scheme.length() + 3);
			isServer = (host.length() == 0);
		} else {
			throw new IllegalArgumentException(name.substring(scheme.length() + 3));
		}

		if (isTCPOBEX) {
			if ((portORuuid == null) || (portORuuid.length() == 0)) {
				portORuuid = String.valueOf(BluetoothConsts.TCP_OBEX_DEFAULT_PORT);
			}
			// else {
			// try {
			// int port = Integer.parseInt(portORuuid);
			// if ((port < 1023) && (port !=
			// BluetoothConsts.TCP_OBEX_DEFAULT_PORT)) {
			// throw new IllegalArgumentException("Port " + portORuuid + " can't
			// be used; the 0-1023 range is reserved");
			// }
			// } catch (NumberFormatException e) {
			// throw new IllegalArgumentException("port " + portORuuid);
			// }
			// }
		}

		if (host == null || portORuuid == null) {
			throw new IllegalArgumentException();
		}

		BluetoothConnectionNotifierParams notifierParams = null;

		BluetoothConnectionParams connectionParams = null;

		boolean isAndroid = values.containsKey(ANDROID);

		int channel = 0;
		if (isServer) {
			if (!allowServer) {
				throw new IllegalArgumentException("Can't use server connection URL");
			}
			if (values.get(NAME) == null) {
				values.put(NAME, "BlueCove");
			} else if (schemeBluetooth) {
				validateBluetoothServiceName((String) values.get(NAME));
			}
			if (schemeBluetooth) {
				notifierParams = new BluetoothConnectionNotifierParams(new UUID(portORuuid, false), paramBoolean(
						values, AUTHENTICATE), paramBoolean(values, ENCRYPT), paramBoolean(values, AUTHORIZE),
						(String) values.get(NAME), paramBoolean(values, MASTER));
				notifierParams.timeouts = timeouts;
				if (notifierParams.encrypt && (!notifierParams.authenticate)) {
					if (values.get(AUTHENTICATE) == null) {
						notifierParams.authenticate = true;
					} else {
						throw new BluetoothConnectionException(BluetoothConnectionException.UNACCEPTABLE_PARAMS,
								"encryption requires authentication");
					}
				}
				if (notifierParams.authorize && (!notifierParams.authenticate)) {
					if (values.get(AUTHENTICATE) == null) {
						notifierParams.authenticate = true;
					} else {
						throw new BluetoothConnectionException(BluetoothConnectionException.UNACCEPTABLE_PARAMS,
								"authorization requires authentication");
					}
				}
				if (isL2CAP) {
					String bluecove_ext_psm = (String) values.get(EXT_BLUECOVE_L2CAP_PSM);
					if (bluecove_ext_psm != null) {
						if ((bluetoothStack.getFeatureSet() & BluetoothStack.FEATURE_ASSIGN_SERVER_PSM) == 0) {
							throw new IllegalArgumentException(EXT_BLUECOVE_L2CAP_PSM + " extension not supported on this stack");
						}
						int psm = Integer.parseInt(bluecove_ext_psm, 16);
						validateL2CAPPSM(psm, bluecove_ext_psm);
						notifierParams.bluecove_ext_psm = psm;
					}
				}
			}
		} else { // (!isServer)
			if (!isAndroid) {
				try {
					channel = Integer.parseInt(portORuuid, isL2CAP ? 16 : 10);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("channel " + portORuuid);
				}
				if (channel < 0) {
					throw new IllegalArgumentException("channel " + portORuuid);
				}
			}
			if (schemeBluetooth) {
				if (!isAndroid) {
					if (isL2CAP) {
						validateL2CAPPSM(channel, portORuuid);
					} else {
						if ((channel < BluetoothConsts.RFCOMM_CHANNEL_MIN)
								|| (channel > BluetoothConsts.RFCOMM_CHANNEL_MAX)) {
							throw new IllegalArgumentException("RFCOMM channel " + portORuuid);
						}
					}
					
					connectionParams = new BluetoothConnectionParams(RemoteDeviceHelper.getAddress(host), channel,
							paramBoolean(values, AUTHENTICATE), paramBoolean(values, ENCRYPT));
				} else {
					try {
						// using reflection not to add a dependency on android module
						connectionParams = (BluetoothConnectionParams) Class.forName("com.intel.bluetooth.AndroidBluetoothConnectionParams").getConstructor(new Class[]{long.class, boolean.class, boolean.class}).newInstance(new Object[]{Long.valueOf(RemoteDeviceHelper.getAddress(host)),
									Boolean.valueOf(paramBoolean(values, AUTHENTICATE)), Boolean.valueOf(paramBoolean(values, ENCRYPT))});
						connectionParams.getClass().getMethod("setServiceUUID", new Class[] {String.class}).invoke(connectionParams, new Object[] {portORuuid});
					} catch (Exception ex) {
						throw new BluetoothConnectionException(BluetoothConnectionException.FAILED_NOINFO, ex.toString());
					}
				}
				
				connectionParams.timeouts = timeouts;
				if (connectionParams.encrypt && (!connectionParams.authenticate)) {
					if (values.get(AUTHENTICATE) == null) {
						connectionParams.authenticate = true;
					} else {
						throw new BluetoothConnectionException(BluetoothConnectionException.UNACCEPTABLE_PARAMS,
								"encryption requires authentication");
					}
				}
				connectionParams.timeout = BlueCoveImpl.getConfigProperty(
						BlueCoveConfigProperties.PROPERTY_CONNECT_TIMEOUT,
						BluetoothConnectionParams.DEFAULT_CONNECT_TIMEOUT);
			}
		}
		OBEXConnectionParams obexConnectionParams = null;

		if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX)
				|| scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX)) {
			obexConnectionParams = new OBEXConnectionParams();
			obexConnectionParams.timeouts = timeouts;
			obexConnectionParams.timeout = BlueCoveImpl.getConfigProperty(
					BlueCoveConfigProperties.PROPERTY_OBEX_TIMEOUT, OBEXConnectionParams.DEFAULT_TIMEOUT);
			obexConnectionParams.mtu = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_OBEX_MTU,
					OBEXConnectionParams.OBEX_DEFAULT_MTU);
		}

		/*
		 * create connection
		 */
		if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM)) {
			if (isServer) {
				return new BluetoothRFCommConnectionNotifier(bluetoothStack, notifierParams);
			} else {
				return new BluetoothRFCommClientConnection(bluetoothStack, connectionParams);
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX)) {
			if (isServer) {
				notifierParams.obex = true;
				return new OBEXSessionNotifierImpl(
						new BluetoothRFCommConnectionNotifier(bluetoothStack, notifierParams), obexConnectionParams);
			} else {
				return new OBEXClientSessionImpl(new BluetoothRFCommClientConnection(bluetoothStack, connectionParams),
						obexConnectionParams);
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_L2CAP)) {
			if (isServer) {
				return new BluetoothL2CAPConnectionNotifier(bluetoothStack, notifierParams, paramL2CAPMTU(values,
						RECEIVE_MTU), paramL2CAPMTU(values, TRANSMIT_MTU));
			} else {
				return new BluetoothL2CAPClientConnection(bluetoothStack, connectionParams, paramL2CAPMTU(values,
						RECEIVE_MTU), paramL2CAPMTU(values, TRANSMIT_MTU));
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX)) {
			if (isServer) {
				try {
					channel = Integer.parseInt(portORuuid);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("port " + portORuuid);
				}
				return new OBEXSessionNotifierImpl(new ServerSocketConnection(channel), obexConnectionParams);
			} else {
				return new OBEXClientSessionImpl(new SocketConnection(host, channel), obexConnectionParams);
			}
		} else if (scheme.equals("socket")) {
			if (isServer) {
				try {
					channel = Integer.parseInt(portORuuid);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("port " + portORuuid);
				}
				return new ServerSocketConnection(channel);
			} else {
				return new SocketConnection(host, channel);
			}
		} else {
			throw new ConnectionNotFoundException("scheme [" + scheme + "]");
		}
	}

	private static void validateL2CAPPSM(int channel, String channelAsString) throws IllegalArgumentException {
		
		if (1==1) return;
		// Valid PSM range: 0x0001-0x0019 (0x1001-0xFFFF dynamically
		// assigned, 0x0019-0x0100 reserved for future use).
		if ((channel < BluetoothConsts.L2CAP_PSM_MIN) || (channel > BluetoothConsts.L2CAP_PSM_MAX)) {
			// PSM 1 discovery, 3 RFCOMM
			throw new IllegalArgumentException("PCM " + channelAsString);
		}
		if ((channel < BluetoothConsts.L2CAP_PSM_MIN_JSR_82)
				&& (!BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_JSR_82_PSM_MINIMUM_OFF, false))) {
			throw new IllegalArgumentException("PCM " + channelAsString + ", PCM values restricted by JSR-82 to minimum "
					+ BluetoothConsts.L2CAP_PSM_MIN_JSR_82 + ", see BlueCoveConfigProperties.PROPERTY_JSR_82_PSM_MINIMUM_OFF");
		}

		// has the 9th bit (0x100) set to zero
		if ((channel & 0x100) != 0) {
			throw new IllegalArgumentException("9th bit set in PCM " + channelAsString);
		}
		// The least significant byte must be odd
		byte lsByte = (byte) (0xFF & channel);
		if ((lsByte % 2) == 0) {
			throw new IllegalArgumentException("PSM value " + channelAsString + " least significant byte must be odd");
		}
		byte msByte = (byte) ((0xFF00 & channel) >> 8);
		if ((msByte % 2) == 1) {
			throw new IllegalArgumentException("PSM value " + channelAsString + " most significant byte must be even");
		}
	}

	private static void validateBluetoothServiceName(String serviceName) {
		if (serviceName.length() == 0) {
			throw new IllegalArgumentException("zero length service name");
		}
		final String allowNameCharactes = " -_";
		for (int i = 0; i < serviceName.length(); i++) {
			char c = serviceName.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
					|| allowNameCharactes.indexOf(c) != -1) {
				continue;
			}
			throw new IllegalArgumentException("Illegal character '" + c + "' in service name");
		}
	}

	private static boolean paramBoolean(Hashtable values, String name) {
		String v = (String) values.get(name);
		if (v == null) {
			return false;
		} else if ("true".equals(v)) {
			return true;
		} else if ("false".equals(v)) {
			return false;
		} else {
			throw new IllegalArgumentException("invalid param value " + name + "=" + v);
		}
	}

	private static int paramL2CAPMTU(Hashtable values, String name) {
		String v = (String) values.get(name);
		if (v == null) {
			if (name.equals(TRANSMIT_MTU)) {
				// This will select RemoteMtu
				return -1;
			} else {
				return L2CAPConnection.DEFAULT_MTU;
			}
		}
		try {
			int mtu = Integer.parseInt(v);
			if (mtu >= L2CAPConnection.MINIMUM_MTU) {
				return mtu;
			}
			if ((mtu > 0) && (mtu < L2CAPConnection.MINIMUM_MTU) && (name.equals(TRANSMIT_MTU))) {
				return L2CAPConnection.MINIMUM_MTU;
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid MTU value " + v);
		}
		throw new IllegalArgumentException("invalid MTU param value " + name + "=" + v);
	}

	/*
	 * Create and open a Connection. Parameters: name - The URL for the connection. mode - The access mode. Returns: A
	 * new Connection object. Throws: IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException
	 * - If the requested connection cannot be made, or the protocol type does not exist. java.io.IOException - If some
	 * other kind of I/O error occurs. SecurityException - If a requested protocol handler is not permitted.
	 */
	public static Connection open(String name, int mode) throws IOException {
		return openImpl(name, mode, false, true);
	}

	/*
	 * Create and open a Connection. Parameters: name - The URL for the connection mode - The access mode timeouts - A
	 * flag to indicate that the caller wants timeout exceptions Returns: A new Connection object Throws:
	 * IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException - if the requested connection
	 * cannot be made, or the protocol type does not exist. java.io.IOException - If some other kind of I/O error
	 * occurs. SecurityException - If a requested protocol handler is not permitted.
	 */
	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		return openImpl(name, mode, timeouts, true);
	}

	/*
	 * Create and open a connection input stream. Parameters: name - The URL for the connection. Returns: A
	 * DataInputStream. Throws: IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException - If
	 * the connection cannot be found. java.io.IOException - If some other kind of I/O error occurs. SecurityException -
	 * If access to the requested stream is not permitted.
	 */
	public static DataInputStream openDataInputStream(String name) throws IOException {
		return new DataInputStream(openInputStream(name));
	}

	/*
	 * Create and open a connection output stream. Parameters: name - The URL for the connection. Returns: A
	 * DataOutputStream. Throws: IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException - If
	 * the connection cannot be found. java.io.IOException - If some other kind of I/O error occurs. SecurityException -
	 * If access to the requested stream is not permitted.
	 */
	public static DataOutputStream openDataOutputStream(String name) throws IOException {
		return new DataOutputStream(openOutputStream(name));
	}

	/*
	 * Create and open a connection input stream. Parameters: name - The URL for the connection. Returns: An
	 * InputStream. Throws: IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException - If the
	 * connection cannot be found. java.io.IOException - If some other kind of I/O error occurs. SecurityException - If
	 * access to the requested stream is not permitted.
	 */
	public static InputStream openInputStream(String name) throws IOException {
		InputConnection con = ((InputConnection) openImpl(name, READ, false, false));
		try {
			return con.openInputStream();
		} finally {
			con.close();
		}
	}

	/*
	 * Create and open a connection output stream. Parameters: name - The URL for the connection. Returns: An
	 * OutputStream. Throws: IllegalArgumentException - If a parameter is invalid. ConnectionNotFoundException - If the
	 * connection cannot be found. java.io.IOException - If some other kind of I/O error occurs. SecurityException - If
	 * access to the requested stream is not permitted.
	 */
	public static OutputStream openOutputStream(String name) throws IOException {
		OutputConnection con = ((OutputConnection) openImpl(name, WRITE, false, false));
		try {
			return con.openOutputStream();
		} finally {
			con.close();
		}
	}
}
