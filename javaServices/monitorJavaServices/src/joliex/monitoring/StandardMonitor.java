/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliex.monitoring;

/**
 *
 * @author claudio guidi 24/01/2012
 * supported events: start_session, stop_session, start_operation, stop_operation
 */
public class StandardMonitor extends AbstractMonitorJavaService {

	@Override
	public void pushEvent( MonitoringEvent e )
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	

}
