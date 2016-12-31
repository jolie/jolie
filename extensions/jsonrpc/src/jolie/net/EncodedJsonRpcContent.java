/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import jolie.StatefulContext;

/**
 *
 * @author martin
 */
public class EncodedJsonRpcContent
{
	private final ByteBuf content;
	private final Charset charset;
	private final StatefulContext context;

	public EncodedJsonRpcContent( ByteBuf content, Charset charset, StatefulContext context )
	{
		this.content = content;
		this.charset = charset;
		this.context = context;
	}

	public Charset charset()
	{
		return charset;
	}

	public ByteBuf content()
	{
		return content;
	}

	public String text() {
		return content().toString( charset );
	}

	public StatefulContext context() {
		return context;
	}
		
}
