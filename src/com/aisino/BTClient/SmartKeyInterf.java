/**
 * 
 */
package com.aisino.BTClient;

/**
 * @author hui
 * 
 */
public interface SmartKeyInterf {

	/**
	 * Sent cmd to smart key.
	 * 
	 * @param src
	 *            The rawdata to send.
	 * @param offset
	 *            The index to send from.
	 * @param length
	 *            Length of the bytes to be sent.
	 * @return
	 */
	public boolean sendCmd(byte[] src, int offset, int length);

	/**
	 * Recv response from the smart key.
	 * 
	 * @return rawdata, null if got none.
	 */
	public byte[] rcvResp();
}
