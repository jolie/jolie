/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as               *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public             *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package com.italianasoftware.echoes.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import joliex.gwt.client.JolieService;
import joliex.gwt.client.Value;

public class Echoes implements EntryPoint
{
	private TextBox location = new TextBox();
	private Label cpLabel = new Label();

	private Value getLocationValue()
	{
		Value ret = new Value();
		ret.getNewChild( "location" ).setValue( location.getText() );
		return ret;
	}
	
	private void showLyrics()
	{
		Value val = getLocationValue();
		lyrics.setText( "Fetching lyrics..." );
		lyricsDialog.center();
		lyricsDialog.show();
		JolieService.Util.getInstance().call(
			"getLyrics", val, new EchoesCallback() {
			@Override
			public void onSuccess( Value response ) {
				lyrics.setHTML( response.strValue().replace( "\n", "<br/>" ) );
				lyricsDialog.center();
			}
		} );
	}
	
	private void waitForStateChange()
	{
		Value val = getLocationValue();
		val.getFirstChild( "logicalClock" ).setValue( logicalClock );
		JolieService.Util.getInstance().call(
			"waitForStateChange", val, new EchoesCallback() {
			@Override
			public void onSuccess( Value response ) {
				setWidgetValues( response );
				logicalClock = response.getFirstChild( "logicalClock" ).intValue();
				waitForStateChange();
			}
		} );
	}
	
	private int sid = -1;
	
	private void startClientSession()
	{
		Value val = getLocationValue();
		JolieService.Util.getInstance().call(
			"startClientSession", val, new EchoesCallback() {
			@Override
			public void onSuccess( Value response ) {
				setWidgetValues( response );
				sid = response.getFirstChild( "sid" ).intValue();
				logicalClock = response.getFirstChild( "logicalClock" ).intValue();
				
				Window.addWindowCloseListener( new WindowCloseListener() {
					public String onWindowClosing() {
						return null;
					}

					public void onWindowClosed() {
						closeClientSession();
					}
				} );
			}
		} );
	}
	
	private int logicalClock = 0;
	
	private void setWidgetValues( Value state )
	{
		state = state.getFirstChild( "state" );
		playlistBox.clear();
		for( Value v : state.getFirstChild( "playlist" ).getChildren( "song" ) ) {
			playlistBox.addItem( v.strValue() );
		}

		cpLabel.setText( state.getFirstChild( "nowPlaying" ).strValue() );

		int volume = state.getFirstChild( "volume" ).intValue();
		volumeMenu.setSelectedIndex( (volume + 4) / 5 );
	}
	
	private void createLyricsDialog()
	{
		lyricsDialog = new DialogBox();
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.setHeight( "100%" );
		vPanel.setHorizontalAlignment( VerticalPanel.ALIGN_CENTER );
		vPanel.setVerticalAlignment( VerticalPanel.ALIGN_MIDDLE );
		lyricsDialog.add( vPanel );
		
		lyrics = new HTML();
		ScrollPanel scrollPanel = new ScrollPanel();
		scrollPanel.setWidth( "300px" );
		scrollPanel.setHeight( "250px" );
		scrollPanel.add( lyrics );
		vPanel.add( scrollPanel );
		
		Button close = new NativeButton( "Close" );
		close.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				lyricsDialog.hide();
			}
		} );
		vPanel.add( close );
	}
	
	private void playSongByIndex( int index )
	{
		Value val = getLocationValue();
		val.getNewChild( "index" ).setValue( index );
		JolieService.Util.getInstance().call(
			"playByIndex", val, new EchoesCallback() {
			@Override
			public void onSuccess( Value response ) {
			}
		} );
	}
	
	private void setVolume( String volume )
	{
		Value val = getLocationValue();
		val.setValue( volume );
		JolieService.Util.getInstance().call(
			"setVolume", val, new EchoesCallback() {
			@Override
			public void onSuccess( Value response ) {
			}
		} );
	}
	
	private void createAboutDialog()
	{
		aboutDialog = new DialogBox();
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.setHorizontalAlignment( VerticalPanel.ALIGN_CENTER );
		aboutDialog.add( vPanel );
		vPanel.add( new HTML( "<strong>Echoes</strong>" ) );
		VerticalPanel cPanel = new VerticalPanel();
		vPanel.add( cPanel );
		cPanel.setHorizontalAlignment( VerticalPanel.ALIGN_CENTER );
		cPanel.addStyleName( "margintop-2em" );
		cPanel.add( new HTML( "(C) 2008, <a href=\"mailto:fmontesi@italianasoftware.com\">Fabrizio Montesi</a>" ) );
		HTML oxygenTeam = new HTML(
			"Echoes logo by <a href=\"http://www.oxygen-icons.org/\" target=\"_new\">the Oxygen team</a>"
		);
		oxygenTeam.addStyleName( "margintop-1em" );
		cPanel.add( oxygenTeam );
		
		HTML license = new HTML(
			"License: <a href=\"http://www.gnu.org/licenses/old-licenses/gpl-2.0.html\" target=\"_new\">GPLv2</a>"
		);
		license.addStyleName( "margintop-1em" );
		cPanel.add( license );
		
		HorizontalPanel hPanel = new HorizontalPanel();
		hPanel.setVerticalAlignment( HorizontalPanel.ALIGN_MIDDLE );
		hPanel.addStyleName( "margintop-1em" );
		HTML poweredBy = new HTML(
			"Powered by: "
		);
		hPanel.add( poweredBy );
		Image jolieImage = new Image( "images/jolie.png" );
		jolieImage.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				Window.open( "http://jolie.sourceforge.net/", "_blank", "" );
			}
		} );
		hPanel.add( jolieImage );
		cPanel.add( hPanel );
		
		Button closeButton = new NativeButton( "Close" );
		closeButton.addStyleName( "margintop-1em" );
		closeButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				aboutDialog.hide();
			}
		} );
		vPanel.add( closeButton );
	}
	
	private DialogBox lyricsDialog;
	private HTML lyrics;
	private ListBox playlistBox;
	private ListBox volumeMenu;
	private DialogBox aboutDialog;
	
	private final static String logoURL = "images/logo.png";
	
	public void onModuleLoad()
	{
		createLyricsDialog();
		
		Image.prefetch( logoURL );
		location.setText( "socket://localhost:10100" );
	
		VerticalPanel mainVPanel = new VerticalPanel();
		mainVPanel.setHorizontalAlignment( VerticalPanel.ALIGN_CENTER );
		RootPanel.get().add( mainVPanel );
		
		HorizontalPanel hPanel;
		hPanel = new HorizontalPanel();
		mainVPanel.add( hPanel );

		VerticalPanel vPanel = new VerticalPanel();
		hPanel.add( vPanel );
		VerticalPanel playlistVPanel = new VerticalPanel();
		playlistVPanel.setHorizontalAlignment( VerticalPanel.ALIGN_CENTER );
		playlistVPanel.setVerticalAlignment( VerticalPanel.ALIGN_MIDDLE );
		hPanel.add( playlistVPanel );
		
		vPanel.setHorizontalAlignment( VerticalPanel.ALIGN_CENTER );
		vPanel.setVerticalAlignment( VerticalPanel.ALIGN_MIDDLE );
		Image logo = new Image( logoURL );
		createAboutDialog();
		logo.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				aboutDialog.center();
				aboutDialog.show();
			}
		} );
		vPanel.add( logo );
		vPanel.addStyleName( "panel-rightborder" );
		
		//hPanel = new HorizontalPanel();
		DisclosurePanel dPanel = new DisclosurePanel( "Location", false );
		VerticalPanel locVPanel = new VerticalPanel();
		locVPanel.setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
		locVPanel.add( location );
		final Button connectButton = new NativeButton( "Connect" );
		connectButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				startClientSession();
				waitForStateChange();
				connectButton.setText( "Connected" );
				connectButton.setEnabled( false );
			}
		} );
		
		dPanel.add( locVPanel );
		//hPanel.add( dPanel );
		vPanel.add( dPanel );
		vPanel.add( connectButton );
		
		//hPanel = new HorizontalPanel();
		Label l = new Label( "Currently playing" );
		l.addStyleName( "label-bold" );
		mainVPanel.add( l );
		mainVPanel.add( cpLabel );
		//mainvPanel.add( hPanel );
		
		HorizontalPanel buttonsPanel = new HorizontalPanel();
		mainVPanel.add( buttonsPanel );
		
		Button playButton = new NativeButton( "Play" );
		playButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				JolieService.Util.getInstance().call(
					"play", getLocationValue(), new EchoesCallback() {
					@Override
					public void onSuccess( Value response ) {
						//updateNowPlaying();
					}
				} );
			}
		} );
		
		Button pauseButton = new NativeButton( "Pause" );
		pauseButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				JolieService.Util.getInstance().call(
					"pause", getLocationValue(), new EchoesCallback() {
					@Override
					public void onSuccess( Value response ) {}
				} );
			}
		} );
		
		Button prevButton = new NativeButton( "&lt;" );
		prevButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				JolieService.Util.getInstance().call(
					"previous", getLocationValue(), new EchoesCallback() {
					@Override
					public void onSuccess( Value response ) {
						//updateNowPlaying();
					}
				} );
			}
		} );
		
		Button nextButton = new NativeButton( "&gt;" );
		nextButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				JolieService.Util.getInstance().call(
					"next", getLocationValue(), new EchoesCallback() {
					@Override
					public void onSuccess( Value response ) {
						//updateNowPlaying();
					}
				} );
			}
		} );

		buttonsPanel.add( prevButton );
		buttonsPanel.add( playButton );
		buttonsPanel.add( pauseButton );
		buttonsPanel.add( nextButton );
		
		playlistBox = new ListBox();
		playlistBox.setVisibleItemCount( 8 );
		playlistVPanel.add( playlistBox );
		playlistBox.setWidth( "250px" );
		
		playlistBox.addClickListener( new ClickListener() {
			public void onClick( Widget widget ) {
				playSongByIndex( playlistBox.getSelectedIndex() );
			}
		} );
		
		hPanel = new HorizontalPanel();

		Button showLyricsButton = new NativeButton( "Show lyrics" );
		showLyricsButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				showLyrics();
			}
		} );
		
		hPanel.add( showLyricsButton );
		playlistVPanel.add( hPanel );
		
		dPanel = new DisclosurePanel( "Volume" );
		playlistVPanel.add( dPanel );
		volumeMenu = new ListBox();
		dPanel.add( volumeMenu );
		addVolumeMenuItems( volumeMenu );
		volumeMenu.addChangeListener( new ChangeListener() {
			public void onChange( Widget arg0 ) {
				String volume = volumeMenu.getValue( volumeMenu.getSelectedIndex() );
				setVolume( volume );
			}
		} );
	}
	
	private void closeClientSession()
	{
		final DialogBox dialog = new DialogBox();
		dialog.add( new Label( "Exiting..." ) );
		dialog.center();
		dialog.show();
		Value v = getLocationValue();
		v.getNewChild( "sid" ).setValue( sid );
		JolieService.Util.getInstance().call(
			"closeClientSession", v, new EchoesCallback() {
			@Override
			public void onSuccess( Value response ) {
				dialog.hide();
			}
		} );
	}
	
	private void addVolumeMenuItems( ListBox volumeMenu )
	{
		volumeMenu.addItem( "0%", "0" );
		volumeMenu.addItem( "5%", "5" );
		volumeMenu.addItem( "10%", "10" );
		volumeMenu.addItem( "15%", "15" );
		volumeMenu.addItem( "20%", "20" );
		volumeMenu.addItem( "25%", "25" );
		volumeMenu.addItem( "30%", "30" );
		volumeMenu.addItem( "35%", "35" );
		volumeMenu.addItem( "40%", "40" );
		volumeMenu.addItem( "45%", "45" );
		volumeMenu.addItem( "50%", "50" );
		volumeMenu.addItem( "55%", "55" );
		volumeMenu.addItem( "60%", "60" );
		volumeMenu.addItem( "65%", "65" );
		volumeMenu.addItem( "70%", "70" );
		volumeMenu.addItem( "75%", "75" );
		volumeMenu.addItem( "80%", "80" );
		volumeMenu.addItem( "85%", "85" );
		volumeMenu.addItem( "90%", "90" );
		volumeMenu.addItem( "95%", "95" );
		volumeMenu.addItem( "100%", "100" );
	}
}
