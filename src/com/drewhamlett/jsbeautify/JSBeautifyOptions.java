package com.drewhamlett.jsbeautify;

import org.openide.util.NbPreferences;

/**
 *
 * @author drewh
 */
public class JSBeautifyOptions {

	private static JSBeautifyOptions INSTANCE;

	public static JSBeautifyOptions getInstance() {

		if ( INSTANCE == null ) {
			INSTANCE = new JSBeautifyOptions();
		}

		return INSTANCE;
	}

	public boolean getOption( String key ) {
		return NbPreferences.forModule( JSBeautifyOptions.class ).getBoolean( key, true );
	}

	public boolean getOption( String key, boolean init ) {
		return NbPreferences.forModule( JSBeautifyOptions.class ).getBoolean( key, init );
	}

	public void setOption( String key, String value ) {
		NbPreferences.forModule( JSBeautifyOptions.class ).put( key, value );
	}

	public void setOption( String key, boolean value ) {
		NbPreferences.forModule( JSBeautifyOptions.class ).putBoolean( key, value );
	}
}
