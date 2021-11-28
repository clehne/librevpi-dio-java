package org.clehne.revpi.dataio;

//Taken from: https://github.com/entropia/libsocket-can-java
//and prepared for https://github.com/clehne/librevpi-dio-java

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

//import org.apache.log4j.Logger;

public final class DataInOut implements Closeable {

	public static final short PICONTROL_LED_A1_MASK	    	= (short) 0x0003;
	public static final short PICONTROL_LED_A1_GREEN	    = (short) 0x0001;
	public static final short PICONTROL_LED_A1_RED          = (short) 0x0002;
	
	public static final short PICONTROL_LED_A2_MASK	    	= (short) 0x000c;
	public static final short PICONTROL_LED_A2_GREEN        = (short) 0x0004;
	public static final short PICONTROL_LED_A2_RED          = (short) 0x0008;

	/** Revpi Connect and Flat */
	public static final short PICONTROL_LED_A3_MASK	    	= (short) 0x0030;
	/** Revpi Connect and Flat */
	public static final short PICONTROL_LED_A3_GREEN        = (short) 0x0010;
	/** Revpi Connect and Flat */
	public static final short PICONTROL_LED_A3_RED          = (short) 0x0020;

	
	/** RevPi Connect only */
	public static final short PICONTROL_X2_DOUT             = (short) 0x0040;
	/** RevPi Connect only */
	public static final short PICONTROL_WD_TRIGGER          = (short) 0x0080;


	/** Revpi Flat only */
	public static final short PICONTROL_LED_A4_MASK	    	= (short) 0x00c0;
	/** Revpi Flat only */
	public static final short PICONTROL_LED_A4_GREEN        = (short) 0x0040;
	/** Revpi Flat only */
	public static final short PICONTROL_LED_A4_RED          = (short) 0x0080;
	
	/** Revpi Flat only */
	public static final short PICONTROL_LED_A5_MASK	    	= (short) 0x0300;
	/** Revpi Flat only */
	public static final short PICONTROL_LED_A5_GREEN        = (short) 0x0100;
	/** Revpi Flat only */
	public static final short PICONTROL_LED_A5_RED          = (short) 0x0200;





//	private static Logger log = Logger.getLogger(DataInOut.class);
	static {
//		System.out.println("Trying to load native library revpi.dataio");
		final String LIB_JNI_REVPI_DIO = "jni_revpi_dio";
		try {
//			System.out.println("Try loadLibrary");
			System.loadLibrary(LIB_JNI_REVPI_DIO);
		} catch (final UnsatisfiedLinkError e) {
			try {
//				System.out.println("Try load from JAR");
				loadLibFromJar(LIB_JNI_REVPI_DIO);
			} catch (final IOException _e) {
//				System.out.println("ERROR: Cannot load CanSocket native library");
				throw new UnsatisfiedLinkError(LIB_JNI_REVPI_DIO);
			}
		}
//		System.out.println("Succesfully loaded native library revpi.dataio");
	}

	private static void copyStream(final InputStream in, final OutputStream out) throws IOException {
		final int BYTE_BUFFER_SIZE = 0x1000;
		final byte[] buffer = new byte[BYTE_BUFFER_SIZE];
		for (int len; (len = in.read(buffer)) != -1;) {
			out.write(buffer, 0, len);
		}
	}

	private static void loadLibFromJar(final String libName) throws IOException {
		Objects.requireNonNull(libName);
		final String fileName = "/lib/lib" + libName + ".so";
//		System.out.println("Load from JAR: " + fileName);
		final FileAttribute<Set<PosixFilePermission>> permissions = PosixFilePermissions
				.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
		final Path tempSo = Files.createTempFile(DataInOut.class.getName(), ".so", permissions);
		try {
			try (final InputStream libstream = DataInOut.class.getResourceAsStream(fileName)) {
				if (libstream == null) {
					throw new FileNotFoundException("jar:*!" + fileName);
				}
				try (final OutputStream fout = Files.newOutputStream(tempSo, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING)) {
					copyStream(libstream, fout);
				}
			}
			System.load(tempSo.toString());
		} finally {
			Files.delete(tempSo);
		}
	}

	private int _fd;

	public DataInOut() {
		try {
			_fd = _openDIO();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		_close(_fd);
	}
	
	/**
	 *	sets channel value for data OUT module channel O1..O16 
	 * @param _channel 1-16
	 * @param value  true or false  
	 * @throws IOException in case of any error
	 */
	public void setDataOut(String _channelAlias, boolean _value) throws IOException {
		int val = _setDataOut(_fd, _channelAlias, (_value?1:0));
		if(val < 0) {
			throw new IOException("Error setting Data OUT " + _channelAlias + " retCode " + val);
		}
	}
	
	/**
	 *	reads in channel value from data OUT module channel O1..O16 
	 * @param _channel 1-16
	 * @return true or false 
	 * @throws IOException in case of any error
	 */
	public boolean getDataOut(String _channelAlias) throws IOException {
		int val = _getDataOut(_fd, _channelAlias);
		if(val < 0) {
			throw new IOException("Error reading Data OUT " + _channelAlias + " retCode " + val);
		}
		return val == 1;
	}
	
	/**
	 *	reads in channel value from data IN module channel I1..I16 
	 * @param _channel 1-16
	 * @return true or false 
	 * @throws IOException in case of any error
	 */
	public boolean getDataIn(String _channelAlias) throws IOException {
		int val = _getDataIn(_fd, _channelAlias);
		if(val < 0) {
			throw new IOException("Error reading Data IN " + _channelAlias + " retCode " + val);
		}
		return val == 1;
	}
	

	private static native int _openDIO() throws IOException;

	private static native void _close(final int fd) throws IOException;

	private static native int _setDataOut(final int fd, String channelAlias, int value);
	private static native int _getDataOut(final int fd, String channelAlias);
	private static native int _getDataIn(final int fd, String channelAlias);
}