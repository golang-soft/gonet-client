package test;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import cmessage.Msgid;
import cmessage.Pub.Ipacket.Builder;

/**
 * 瀹㈡埛绔�
 */
public class Client {
	private static Logger logger = Logger.getLogger(Client.class.getName());
	private static String serverInetAddress = "127.0.0.1";
	private static int serverPort = 31700;
	private static Socket client = null;
	private static OutputStream os = null;
	private static InputStream is = null;
	private static boolean alive = true;
	private static int Default_Ipacket_Stx = 39;
	private static int Default_Ipacket_Ckx = 114;
	private static String BUILD_NO = "1,5,1,1";

	/**
	 * 瀹㈡埛绔繛鎺ユ湇鍔″櫒
	 */
	public void init() {
		System.out.println("鍚姩");
		try {
			// 寤虹珛杩炴帴
			client = new Socket(serverInetAddress, serverPort);
			// 鏁版嵁娴佸彂閫佹暟鎹�
			os = client.getOutputStream();
			// 鏁版嵁娴佹帴鏀舵暟鎹�
			is = client.getInputStream();
			byte[] b = new byte[1024];
			int length = 0;
			while (alive) {
				// 鎺ユ敹浠庢湇鍔″櫒鍙戦�佸洖鏉ョ殑娑堟伅
				length = is.read(b);
				if (length != -1) {
					onMsg(b);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// 鍏虫祦
				os.close();
				client.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 鍏抽棴瀹㈡埛绔�
	 */
	public void close() {
		sendMsg("{\"type\":\"CLOSE\"}");
		alive = false;
	}

	public Builder getPacket(Msgid.MessageID id, int destservertype) {
		Builder packet = cmessage.Pub.Ipacket.newBuilder();
		packet.setStx(Default_Ipacket_Stx);
		packet.setCkx(Default_Ipacket_Ckx);
		packet.setId(id);
		return packet;
	}

	/*
	 * 鍚▼绋嬪簭璐﹀彿鐧婚檰
	 */
	public void AccountLogin() {
		cmessage.Client.C_A_LoginRequest.Builder personBuilder = cmessage.Client.C_A_LoginRequest.newBuilder();
		personBuilder.setPacketHead(getPacket(Msgid.MessageID.MSG_C_A_LoginRequest, 0));
		personBuilder.setAccountName("admin");
//		personBuilder.setSocketId(0);
		personBuilder.setBuildNo(BUILD_NO);
		cmessage.Client.C_A_LoginRequest person = personBuilder.build();
		byte[] buff = person.toByteArray();
		System.out.println("鐧婚檰");
		sendMsg("C_A_LoginRequest", buff);
	}

	/**
	 * 鍙戦�佹秷鎭�
	 */
	public void sendMsg(String msg) {
		try {
			// 璋冪敤鍙戦��
			os.write(msg.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMsg(String packedHead, byte[] buff) {
		try {
			// 璋冪敤鍙戦��
			System.out.println("鍙戝寘");
			byte[] data = GetData(packedHead, buff);
			System.out.println(new String(data));
			os.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] GetData(String packedHead, byte[] buff) {
		byte[] end = "鈾♀櫋鈾�".getBytes();
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update(packedHead.toLowerCase().getBytes());
		int crcint = new Long(crc.getValue()).intValue();
		byte[] idStr = toLH(crcint);
		byte[] data = byteMerger(idStr, buff, end);
		return data;
	}

	public byte[] byteMerger(byte[] bt1, byte[] bt2, byte[] bt3) {
		byte[] bt4 = new byte[bt1.length + bt2.length];
		System.arraycopy(bt1, 0, bt4, 0, bt1.length);
		System.arraycopy(bt2, 0, bt4, bt1.length, bt2.length);
		byte[] bt5 = new byte[bt4.length + bt3.length];
		System.arraycopy(bt4, 0, bt5, 0, bt4.length);
		System.arraycopy(bt3, 0, bt5, bt4.length, bt3.length);
		return bt5;
	}

	public byte[] toLH(int n) {
		byte[] b = new byte[4];
		b[0] = (byte) (n & 0xff);
		b[1] = (byte) (n >> 8 & 0xff);
		b[2] = (byte) (n >> 16 & 0xff);
		b[3] = (byte) (n >> 24 & 0xff);
		return b;
	}

	/**
	 * 鏀跺埌娑堟伅鐨勫洖璋�
	 */
	private void onMsg(byte[] buff) {
//		logger.debug(new String(buff));
	}
}