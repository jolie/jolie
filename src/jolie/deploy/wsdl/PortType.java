package jolie.deploy.wsdl;

import java.util.Vector;

import jolie.Operation;


public class PortType
{
	private String id;
	private Vector< Operation > operations;
	
	public Vector< Operation > operations()
	{
		return operations;
	}
	
	public String id()
	{
		return id;
	}
}