import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import cmessage.Msgid;
import cmessage.Pub.Ipacket.Builder;

public class Connect {
	private static final ThreadLocal<Socket> threadConnect = new ThreadLocal<Socket>();
	private static Logger logger = Logger.getLogger(Connect.class.getName());
	private static final String HOST = "localhost";

	private static final int PORT = 3000;

	private static Socket client;

	private static OutputStream outStr = null;
	private static InputStream inStr = null;
	private static byte[] m_pInBuffer = new byte[1024 * 4];
	private static Thread tRecv = new Thread(new RecvThread());
	private static Thread tKeep = new Thread(new KeepThread());
	private static int Default_Ipacket_Stx = 39;
	private static int Default_Ipacket_Ckx = 114;
	private static String BUILD_NO = "1,5,1,1";
	private static byte[] endByte = "".getBytes();

	public static void connect() throws UnknownHostException, IOException {
		client = threadConnect.get();
		if (client == null) {
			client = new Socket(HOST, PORT);
			threadConnect.set(client);
			// tKeep.start();
		}
		outStr = client.getOutputStream();
		inStr = client.getInputStream();
	}

	private static int getCrc(String packetStr) {
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update(packetStr.toLowerCase().getBytes());
		int crcint = new Long(crc.getValue()).intValue();
		System.out.println(crcint);
		// byte[] dd=toLH(crcint);
		// int ss=Bytes2Int_LE(dd);
		return crcint;
	}

	public static void sendMsg(String packedHead, byte[] buff) {
		try {
			// 璋冪敤鍙戦��
			byte[] data = GetData(packedHead, buff);
			outStr.write(data);
			outStr.flush();
//        	client.shutdownOutput();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void disconnect() {
		try {
			outStr.close();
			inStr.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Builder getPacket(Msgid.MessageID id) {
		Builder packet = cmessage.Pub.Ipacket.newBuilder();
		packet.setStx(Default_Ipacket_Stx);
		packet.setCkx(Default_Ipacket_Ckx);
		packet.setId(id);
		return packet;
	}
	
	public static void AccountLogin() {
		cmessage.Client.C_A_LoginRequest.Builder personBuilder = cmessage.Client.C_A_LoginRequest.newBuilder();
		personBuilder.setPacketHead(getPacket(cmessage.Msgid.MessageID.MSG_C_A_LoginRequest));
		personBuilder.setAccountName("admin");
		personBuilder.setBuildNo(BUILD_NO);
		personBuilder.setKey(Default_Ipacket_Ckx);
		
		cmessage.Client.C_A_LoginRequest person = personBuilder.build();
		byte[] buff = person.toByteArray();
		sendMsg("C_A_LoginRequest", buff);
	}
	
	public static byte[] GetData(String packedHead, byte[] buff) {
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update(packedHead.toLowerCase().getBytes());
		int crcint = new Long(crc.getValue()).intValue();
		System.out.println(crcint);
		byte[] idStr = toLH(crcint);
		byte[] data = byteMerger(idStr, buff, endByte);
		return data;
	}
	
	public static byte[] byteMerger(byte[] bt1, byte[] bt2, byte[] bt3) {
		byte[] bt4 = new byte[4+ bt1.length + bt2.length];
		int length = 4+   bt2.length;
		byte[] bt0 = toLH(length);
		System.arraycopy(bt0, 0, bt4, 0, 4);
		System.arraycopy(bt1, 0, bt4, 4, bt1.length);
		System.arraycopy(bt2, 0, bt4, bt1.length+4, bt2.length);
		return bt4;
	}
	/**
     * int到byte[] 由高位到低位
     * @param i 需要转换为byte数组的整行值。
     * @return byte数组
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }
    
	public static byte[] toLH(int n) {
		byte[] b = new byte[4];
		b[0] = (byte) (n & 0xff);
		b[1] = (byte) (n >> 8 & 0xff);
		b[2] = (byte) (n >> 16 & 0xff);
		b[3] = (byte) (n >> 24 & 0xff);
		return b;
	}
	
	private static class KeepThread implements Runnable {
		public void run() {
			try {
				System.out.println("=====================寮�濮嬪彂閫佸績璺冲寘==============");
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("鍙戦�佸績璺虫暟鎹寘");
					outStr.write("send heart beat data package !".getBytes());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private static class RecvThread implements Runnable {
		@SuppressWarnings("unused")
		public void run() {
			try {
				System.out.println("寮�濮嬫帴鏀舵暟鎹�");
				int nPacketSize = 0;
				while (true) {
					byte[] b = new byte[1024];
					int r = inStr.read(b);
					if (r > -1) {
						int endIndex = seekToTcpEnd(0, b);
						if (endIndex <= 0) {// 娌℃敹鍒板叏鍖咃紝鏆傚瓨鏁版嵁
							System.arraycopy(b, 0, m_pInBuffer, nPacketSize, b.length);
							nPacketSize += b.length;
						}
						if (endIndex > 0) {// 鎵惧埌鏁版嵁鍖呯粨鏉熺鍙�
							System.arraycopy(b, 0, m_pInBuffer, nPacketSize, endIndex + endByte.length);// 缁勫悎瀹屾暣鏁版嵁鍖�
							int nCurSize = 0;// 鏁版嵁鍖呯粨鏉熶綅缃�
							byte[] crc = new byte[4];// 鍖呭ごbyte
							System.arraycopy(m_pInBuffer, 0, crc, 0, crc.length);// 鍙栧嚭鍖呭ご
							nCurSize = seekToTcpEnd(0, m_pInBuffer);// 鑾峰彇灏鹃儴鏍囪瘑鍦╞yte[]浣嶇疆
							byte[] packet = new byte[nCurSize - crc.length];// 鍒濆鍖杙rotobuf鏁版嵁澶у皬
							System.arraycopy(m_pInBuffer, crc.length, packet, 0, nCurSize - crc.length);// 鎴彇protobuf娴�
							String packetStr = "";
							int packetID = Bytes2Int_LE(crc);
							System.out.println(packetID);
//                    		if(m_PacketCreateMap.containsKey(packetID)){//鑾峰彇鍖呭ごID
//                    			packetStr=m_PacketCreateMap.get(packetID).toString();
//                    		}
							switch (packetStr) {// 鍒ゆ柇鍖呭ご锛岃В鏋愭暟鎹�
							case "W_C_SelectPlayerResponse":
								cmessage.Client.W_C_SelectPlayerResponse m = cmessage.Client.W_C_SelectPlayerResponse
										.parseFrom(packet);
								List<cmessage.Pub.PlayerData> list = m.getPlayerDataList();
								// logger.debug(list.get(0).getPlayerName());
								break;

							default:
								break;
							}

						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public static int seekToTcpEnd(int id, byte[] buff) {
		int i = ByteIndexOf(buff, endByte);
		return i;
	}

	public static int ByteIndexOf(byte[] srcBytes, byte[] searchBytes) {
		if (srcBytes == null) {
			return -1;
		}
		if (searchBytes == null) {
			return -1;
		}
		if (srcBytes.length == 0) {
			return -1;
		}
		if (searchBytes.length == 0) {
			return -1;
		}
		if (srcBytes.length < searchBytes.length) {
			return -1;
		}
		for (int i = 0; i < srcBytes.length - searchBytes.length; i++) {
			if (srcBytes[i] == searchBytes[0]) {
				if (searchBytes.length == 1) {
					return i;
				}
				boolean flag = true;
				for (int j = 1; j < searchBytes.length; j++) {
					if (srcBytes[i + j] != searchBytes[j]) {
						flag = false;
						break;
					}
				}
				if (flag) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * 杞崲byte鏁扮粍涓篿nt锛堝皬绔級
	 * 
	 * @return
	 * @note 鏁扮粍闀垮害鑷冲皯涓�4锛屾寜灏忕鏂瑰紡杞崲,鍗充紶鍏ョ殑bytes鏄皬绔殑锛屾寜杩欎釜瑙勫緥缁勭粐鎴恑nt
	 */
	public static int Bytes2Int_LE(byte[] bytes) {
		if (bytes.length < 4)
			return -1;
		int iRst = (bytes[0] & 0xFF);
		iRst |= (bytes[1] & 0xFF) << 8;
		iRst |= (bytes[2] & 0xFF) << 16;
		iRst |= (bytes[3] & 0xFF) << 24;

		return iRst;
	}

	/**
	 * 杞崲byte鏁扮粍涓篿nt锛堝ぇ绔級
	 * 
	 * @return
	 * @note 鏁扮粍闀垮害鑷冲皯涓�4锛屾寜灏忕鏂瑰紡杞崲锛屽嵆浼犲叆鐨刡ytes鏄ぇ绔殑锛屾寜杩欎釜瑙勫緥缁勭粐鎴恑nt
	 */
	public static int Bytes2Int_BE(byte[] bytes) {
		if (bytes.length < 4)
			return -1;
		int iRst = (bytes[0] << 24) & 0xFF;
		iRst |= (bytes[1] << 16) & 0xFF;
		iRst |= (bytes[2] << 8) & 0xFF;
		iRst |= bytes[3] & 0xFF;

		return iRst;
	}

	public static void main(String[] args) throws InterruptedException {
		try {
			Connect.connect();
			tRecv.start();
			Connect.AccountLogin();
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					System.out.println();
				}
			}, 1000, 5000);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}