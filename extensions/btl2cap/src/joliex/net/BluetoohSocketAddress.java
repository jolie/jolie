/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.net;

import java.net.SocketAddress;
import java.net.URI;

/**
 *
 * @author martin
 */
public class BluetoohSocketAddress extends SocketAddress
{
	
	private final URI uri;

	public BluetoohSocketAddress( URI uri )
	{
		this.uri = uri;
	}

	public URI uri() {
		return uri;
	}
	
}
