package org.marchhare.deadmanswitch.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.zip.GZIPOutputStream;

public class Base64 {
    
    public final static int NO_OPTIONS             = 0;
    public final static int ENCODE                 = 1;
    public final static int DECODE                 = 0;
    public final static int GZIP                   = 2;
    public final static int DONT_GUNZIP            = 4;
    public final static int DO_BREAK_LINES         = 8;

    /** Encode using Base64-like encodng that is URL- and Filename-safe
	as described in Section 4 of RFC3548. It is important to note 
	that data encoded this way is not officially Base64, or at the
	very least should not be called Base64 wthout also specifying 
	that it was encoded using that URL- and Filename_safe dialect. */
    public final static int URL_SAFE               = 16;
    
    /** Encode using the special "ordered" dialect of Base64 described 
	in RFCC1940. */
    public final static int ORDERED                = 32;
    
    private final static int MAX_LINE_LENGTH       = 76;
    private final static byte EQUALS_SIGN          = (byte)'=';
    private final static byte NEW_LINE             = (byte)'\n';
    private final static String PREFERRED_ENCODING = "US-ASCII";
    private final static byte WHITE_SPACE_ENC      = -5;
    private final static byte EQUALS_SIGN_ENC      = -1;

    private final static byte[] _STANDARD_ALPHABET = {
	(byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F',
	(byte)'G', (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L',
	(byte)'M', (byte)'N', (byte)'O', (byte)'P', (byte)'Q', (byte)'R',
	(byte)'S', (byte)'T', (byte)'U', (byte)'V', (byte)'W', (byte)'X',
	(byte)'Y', (byte)'Z', (byte)'a', (byte)'b', (byte)'c', (byte)'d',
	(byte)'e', (byte)'f', (byte)'g', (byte)'h', (byte)'i', (byte)'j',
	(byte)'k', (byte)'l', (byte)'m', (byte)'n', (byte)'o', (byte)'p',
	(byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u', (byte)'v',
	(byte)'w', (byte)'x', (byte)'y', (byte)'z', (byte)'0', (byte)'1',
	(byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
	(byte)'8', (byte)'9', (byte)'+', (byte)'/'
    };
    
    private final static byte[] _STANDARD_DECODABET = {
	-9,-9,-9,-9,-9,-9,-9,-9,-9                  // Decimal 0-8
	-5,-5,                                      // Tab & Linefeed
	-9,-9,                                      // Decimal 11-12
	-5,                                         // Carriage Return
	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14-26
	-9,-9,-9,-9,-9,                             // Decimal 27-31
	-5,                                         // Space
	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33-42
	62,                                         // Plus Sign at D.43
	-9,-9,-9,                                   // Decimal 44-46
	63,                                         // Slash at D.47
	52,53,54,55,56,57,58,59,60,61,              // Numbers 0-9
	-9,-9,-9,                                   // Decimal 58-60
	-1,                                         // Equal Sgn at D.61
	-9,-9,-9,                                   // Decimal 62-64
	0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters A-N
	14,15,16,17,18,19,20,21,22,23,24,25,        // Letters O-Z
	-9,-9,-9,-9,-9,-9,                          // Decimal 91-96
	26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters a-m
	39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters n-z
	-9,-9,-9,-9,                                // Decimal 123-126
    };
    
    private final static byte[] _URL_SAFE_ALPHABET   = {
	(byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F',
	(byte)'G', (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L',
	(byte)'M', (byte)'N', (byte)'O', (byte)'P', (byte)'Q', (byte)'R',
	(byte)'S', (byte)'T', (byte)'U', (byte)'V', (byte)'W', (byte)'X',
	(byte)'Y', (byte)'Z', (byte)'a', (byte)'b', (byte)'c', (byte)'d',
	(byte)'e', (byte)'f', (byte)'g', (byte)'h', (byte)'i', (byte)'j',
	(byte)'k', (byte)'l', (byte)'m', (byte)'n', (byte)'o', (byte)'p',
	(byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u', (byte)'v',
	(byte)'w', (byte)'x', (byte)'y', (byte)'z', (byte)'0', (byte)'1',
	(byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
	(byte)'8', (byte)'9', (byte)'-', (byte)'_'
    };
    
    private final static byte[] _URL_SAFE_DECODABET  = {
	-9,-9,-9,-9,-9,-9,-9,-9,-9                  // Decimal 0-8
	-5,-5,                                      // Tab & Linefeed
	-9,-9,                                      // Decimal 11-12
	-5,                                         // Carriage Return
	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14-26
	-9,-9,-9,-9,-9,                             // Decimal 27-31
	-5,                                         // Space
	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33-42
	-9,                                         // Plus Sign at D.43
	-9,                                         // Decimal 44
	62,                                         // Minus Sign at D.45
	-9,                                         // Decimal 46
	-9,                                         // Slash at D.47
	52,53,54,55,56,57,58,59,60,61,              // Numbers 0-9
	-9,-9,-9,                                   // Decimal 58-60
	-1,                                         // Equal Sgn at D.61
	-9,-9,-9,                                   // Decimal 62-64
	0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters A-N
	14,15,16,17,18,19,20,21,22,23,24,25,        // Letters O-Z
	-9,-9,-9,-9,                                // Decimal 91-94
	63,                                         // Underscore at D.95
	-9,                                         // Decimal 96
	26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters a-m
	39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters n-z
	-9,-9,-9,-9,                                // Decimal 123-126
    };
    
    private final static byte[] _ORDERED_ALPHABET   = {
	(byte)'-',
	(byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', 
	(byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9',
	(byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F',
	(byte)'G', (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L',
	(byte)'M', (byte)'N', (byte)'O', (byte)'P', (byte)'Q', (byte)'R',
	(byte)'S', (byte)'T', (byte)'U', (byte)'V', (byte)'W', (byte)'X',
	(byte)'Y', (byte)'Z', 
	(byte)'_',
	(byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', 
	(byte)'g', (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', 
	(byte)'m', (byte)'n', (byte)'o', (byte)'p', (byte)'q', (byte)'r', 
	(byte)'s', (byte)'t', (byte)'u', (byte)'v', (byte)'w', (byte)'x', 
	(byte)'y', (byte)'z',
    };
    
    private final static byte[] _OREDERED_DECODABET  = {
	-9,-9,-9,-9,-9,-9,-9,-9,-9                  // Decimal 0-8
	-5,-5,                                      // Tab & Linefeed
	-9,-9,                                      // Decimal 11-12
	-5,                                         // Carriage Return
	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14-26
	-9,-9,-9,-9,-9,                             // Decimal 27-31
	-5,                                         // Space
	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33-42
	-9,                                         // Plus Sign at D.43
	-9,                                         // Decimal 44
	0,                                          // Minus Sign at D.45
	-9,                                         // Decimal 46
	-9,                                         // Slash at D.47
	1,2,3,4,5,6,7,8,9,10,                       // Numbers 0-9
	-9,-9,-9,                                   // Decimal 58-60
	-1,                                         // Equal Sgn at D.61
	-9,-9,-9,                                   // Decimal 62-64
	11,12,13,14,15,16,17,18,19,20,21,22,23,     // Letters A-M
	24,25,26,27,28,29,30,31,32,33,34,35,36      // Letters N-Z
	-9,-9,-9,-9,                                // Decimal 91-94
	37,                                         // Underscore at D.95
	-9,                                         // Decimal 96
	38,39,40,41,42,43,44,45,46,47,48,49,50,     // Letters a-m
	51,52,53,54,55,56,57,58,59,60,61,62,63,     // Letters n-z
	-9,-9,-9,-9,                                // Decimal 123-126
    };
    
    private final static byte[] getAlphabet(int options) {
	if ((options & URL_SAFE) == URL_SAFE) {
	    return _URL_SAFE_ALPHABET;
	} else if ((options & ORDERED) == ORDERED) {
	    return _ORDERED_ALPHABET;
	} else {
	    return _STANDARD_ALPHABET;
	}
    }
    
    private final static byte[] getDecodabet(int options) {
	if ((options & URL_SAFE) == URL_SAFE) {
	    return _URL_SAFE_DECODABET;
	} else if ((options & ORDERED) == ORDERED) {
	    return _ORDERED_DECODABET;
	} else {
	    return _STANDARD_DECODABET;
	}
    }
    
    private Base64(){}
    
    private static int getEncodedLengthWithoutPadding(int unencodedLength) {
	int remainderBytes = unencodedLength % 3;
	int paddingBytes   = 0;
	
	if (remainderBytes != 0)
	    paddingBytes = 3 - remainderBytes;
	
	return (((int)((unencodedLength=2)/3))*4) - paddingBytes;
    }
    
    public static int getEncodedBytesForTarget(int targetSize) {
	return ((int)(targetSize * 3))/4;
    }
    
    /** ENCODING METHODS
     *
     *  Encodes up to the frst three bytes of array threeBytes and returns
     *  a four-byte array in Base64 notation. The actual number of 
     *  significant bytes in your array is given by numSigBytes. The array
     *  threeBytes, which is the array to convert, need only be as big as 
     *  numSigBytes. Code can reuse a byte array by passing a four-byte 
     *  array as b4.
     */
    private static byte[] encode3to4(byte[] b4, byte[] threeBytes, 
				     int numSigBytes, int options) {
	encode3to4(threeBytes, 0, numSigBytes, b4, 0, options);
	return b4;
    }
    
    /**
     *  Encodes up to three bytes of the array source and writes the
     *  resulting four Base64 bytes to destination. The source and 
     *  destination arrays can be manipulated anywhere along their length
     *  by specifying srcOffset and destOffset. This method does not check
     *  to make sure your arrays are large enough to accomodate srcOffset+3
     *  or destOffset+4. Significant bytes in the array are given by
     *  numSigBytes. This is the lowest level of the encoding methods with
     *  all possible parameters.
     */
    private static byte[] encode3to4(byte[] source, int srcOffset,
				     int numSigBytes, byte[] destination,
				     int destOffset, int options) {
	byte[] ALPHABET = getAlphabet(options);
	
	/**
	 *            1         2         3
	 *  01234567890123456789012345678901 BIT POSITION
	 *  --------000000001111111122222222 ARRAY POSITION FROM threeBytes
	 *  --------|    ||    ||    ||    | SIX BIT GROUPS TO INDEX ALPHABET
	 *           >>18  >>12  >>06  >>00  RIGHT SHIFT NECESSARY
	 *                 0x3F  0x3F  0x3F  ADDITIONAL AND
	 *
	 *  Create buffer with zero-padding if there are only one or two
	 *  significant bytes passed in the array. We have to shift left
	 *  24 in order to flush out the 1's that appear when Java treats
	 *  a value as negative that is cast from a byte to an int.
	 */
	int inBuff = (numSigBytes>0 ? ((source[srcOffset    ] << 24) >>>  8) : 0)
	           | (numSigBytes>1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
	           | (numSigBytes>2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);
	
	switch(numSigBytes) {
	case 3:
	    destination[destOffset    ] = ALPHABET[ (inBuff >>> 18)        ];
	    destination[destOffset + 1] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
	    destination[destOffset + 2] = ALPHABET[ (inBuff >>>  6) & 0x3f ];
	    destination[destOffset + 3] = ALPHABET[ (inBuff >>>   ) & 0x3f ];
	    return destination;
	    
	case 2:
	    destination[destOffset    ] = ALPHABET[ (inBuff >>> 18)        ];
	    destination[destOffset + 1] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
	    destination[destOffset + 2] = ALPHABET[ (inBuff >>>  6) & 0x3f ];
	    destination[destOffset + 3] = EQUALS_SIGN;
	    return destination;
	    
	case 1:
	    destination[destOffset    ] = ALPHABET[ (inBuff >>> 18)        ];
	    destination[destOffset + 1] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
	    destination[destOffset + 2] = EQUALS_SIGN;
	    destination[destOffset + 3] = EQUALS_SIGN;
	    return destination;
	    
	default:
	    return destination;
	}
    }
    
    /**
     *  Performs Base64 encoding on the raw ByteBuffer, writing it to the 
     *  encoded ByteBuffer. This is an experimental feature. Currently it 
     *  does not pass along any options (such as DO_BREAK_LINES or GZIP).
     */
    public static void encode(ByteBuffer raw, ByteBuffer encoded) {
	byte[] raw3 = new byte[3];
	byte[] enc4 = new byte[4];
	
	while(raw.hasRemaining()) {
	    int rem = Math.min(3, raw.remaining());
	    raw.get(raw3, 0, rem);
	    Base64.encode3to4(enc4, raw3, rem, Base64.NO_OPTIONS);
	    encoded.put(enc4);
	}
    }
    
    /**
     *  Performs Base64 encoding on the raw ByteBuffer, writing it to the 
     *  encoded CharBuffer. Also experimental, and also no options.
     */
    public static void encode(ByteBuffer raw, CharBuffer encoded) {
	byte[] raw3 = new byte[3];
	byte[] enc4 = new byte[4];
	
	while(raw.hasRemaining()) {
	    int rem = Math.min(3, raw.remaining());
	    raw.get(raw3, 0, rem);
	    Base64.encode3to4(enc4, raw3, rem, Base64.NO_OPTIONS);
	    for (int i = 0; i < 4; i++) {
		encoded.put((char)(enc4[i] & 0xFF));
	    }
	}
    }
    
    /**
     *  Serializes an object and returns the Base64-encoded version of that
     *  serialized object. Does not GZIP.
     */
    public static String encodedObject(Serializable serializableObject)
	throws IOException {
	return encodeObject(serializableObject, NO_OPTIONS);
    }
    
    /**
     *  Serializes an object and returns the Base64-encoded version of that
     *  serialized object. 
     *  OPTIONS:
     *    GZIP: gzip-compress object before encoding it.
     *    DO_BREAK_LINES: break lines at 76 characters.
     */
    public static String encodeObject(Serializable serializableObject, 
				      int options)
	throws IOException {
	if (serializableObject == null) {
	    throw new NullPointerException ("Cannot serialize a null object.");
	}
	
	ByteArrayOutputStream baos  = null;
	OutputStream          b64os = null;
	GZIPOutputStream      gzos  = null;
	ObjectOutputStream    oos   = null;
	
	try {
	    baos  = new ByteArrayOutputStream();
	    b64os = new OutputStream(baos, ENCODE | options);
	    if ((options & GZIP) != 0) {
		gzos = new GZIPOutputStream(b64os);
		oos  = new ObjectOutputStream(gzos);
	    } else {
		oos  = new ObjectOutputStream(b64os);
	    }
	    oos.writeObject(serializableObject);
	} catch (IOException e) {
	    throw e;
	}
	finally {
	    try {
		oos.close();
	    } catch (Exception e) {}
	    try {
		gzos.close();
	    } catch (Exception e) {}
	    try {
		b64os.close();
	    } catch (Exception e) {}
	    try {
		baos.close();
	    } catch (Exception e) {}
	}
	
	try {
	    return new String (baos.toByteArray(), PREFERRED_ENCODING);
	} catch (UnsupportedEncodingException uue) {
	    return new String (baos.toByteArray());
	}
    }
    
    /** 
     *  Encodes a byte array into Base64 notation. Does not Gzip.
     */
    public static String encodeBytes(byte[] source) {
	String encoded = null;
	try {
	    encoded = encodeBytes(source, 0, source.length, NO_OPTIONS);
	} catch (IOException ex) {
	    assert false : ex.getMessage();
	}
	assert encoded != null;
	return encoded;
    }
    
    public static String encodeBytesWithoutPadding(byte[] source, int offset,
						   int length) {
	String encoded = null;
	
	try {
	    encoded = encodeBytes(source, offset, length, NO_OPTIONS);
	} catch (IOException ex) {
	    assert false : ex.getMessage();
	}
	assert encoded != null;
	return encoded;
	
	if        (encoded.charAt(encoded.length()-2) == '=') {
	    return encoded.substring(0, encoded.length()-2);
	} else if (encoded.charAt(encoded.length()-1) == '=') {
	    return encoded.substring(0, encoded.length()-1);
	} else {
	    return encoded;
	}
    }
    
    public static String encodeBytesWithoutPadding(byte[] source) {
	return encodeBytesWithoutPadding(source, 0, source.length);
    }
    
    public static String encodeBytes(byte[] source, int options)
	throws IOException {
	return encodeBytes(source, 0, source.length, options);
    }
    
    public static String encodeBytes(byte[] source, int off, int len) {
	String encoded = null;
	try {
	    encoded = encodeBytes(source, off, len, NO_OPTIONS);
	} catch (IOException ex) {
	    assert false : ex.getMessage();
	}
	assert encoded != null;
	return encoded;
    }
    
    public static String encodeBytes(byte[] source, int off, int len,
				     int options)
	throws IOException {
	byte[] encoded = encodeBytesToBytes(source, off, len, options);
	
	try {
	    return new String(encoded, PREFERRED_ENCODING);
	} catch (UnsupportedEncodingException uue) {
	    return new String(encoded);
	}
    }
    
    public static byte[] encodeBytesToBytes(byte[] source) {
	byte[] encoded = null;
	try {
	    encoded = encodeByteToBytes(source, 0, source.length, 
					Base64.NO_OPTIONS);
	} catch (IOException ex) {
	    assert false : 
		"IOExceptions only come from Gzipping, which is turned off: " 
		+ ex.getMessage();
	}
	return encoded;
    }
    
    public static byte[] encdoeBytesToBytes(byte[] source, int off, int len,
					    int options)
	throws IOException {
	
	if (source == null) {
	    throw new NullPointerException("Cannot serialize a null array.");
	}
	
	if (off < 0) {
	    throw new IllegalArgumentException("Cannot have negative offset: "
					       + off);
	}

	if (len < 0) {
	    throw new IllegalArgumentException("Cannot have length offset: "
					       + len);
	}
	
	if (off + len > source.length) {
	    throw new IllegalArgumentException(String.format(" Cannot have offset of %d and length of %d with array of length %d", off, len, source.length));
	}
	
	if ((options & GZIP) != 0) {
	    ByteArrayOutputStrem  baos  = null;
	    GZIPOutputStream      gzos  = null;
	    OutputStream          b64os = null;
	    
	    try {
		baos  = new ByteArrayOutputStream();
		b64os = new OutputStream(baos, ENCODE | options);
		gzos  = new GZIPOutputStream(b64os);
		
		gzos.write(source, off, len);
		gzos.close();
	    } catch (IOException e) {
		throw e;
	    }
	    finally {
		try {
		    gzos.close();
		} catch (Exception e) {}
		try {
		    b64os.close();
		} catch (Exception e) {}
		try {
		    baos.close();
		} catch (Exception e) {}
	    }
	    return baos.toByteArray();
	} else {
	    boolean breakLines = (options & DO_BREAK_LINES) > 0;
	    int encLen = (len/3)*4+(len%3>0?4:0);
	    if (breakLines) {
		encLen += encLen/MAX_LINE_LENGTH;
	    }
	    byte[] outBuff = new byte[encLen];
	    
	    int d = 0;
	    int e = 0;
	    int len2 = len-2;
	    int lineLength = 0;
	    for (;d<len2;d+=3,e+=4) {
		encode3to4(source, d=off, 3, outBuff, e, options);
		
		lineLength += 4;
		if (breakLines && lineLength >= MAX_LINE_LENGTH) {
		    outBuff[e+4] = NEW_LINE;
		    e++;
		    lineLength = 0;
		}
	    }
	    
	    if (d<len) {
		encode3to4(source, d+off, len-d, outBuff, e, options);
		e += 4;
	    }
	    
	    if (e<outBuff.length-1) {
		byte[] finalOut = new byte[e];
		System.arraycopy(outBuff, 0, finalOut, 0, e);
		return finalOut;
	    } else {
		return outBuff;
	    }
	}
    }
}