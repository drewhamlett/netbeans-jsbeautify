/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.drewhamlett.jsbeautify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import javax.swing.JEditorPane;
import javax.swing.text.StyledDocument;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.openide.ErrorManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 *
 * @author drewh
 */
public class JSBeautifyRunnable implements Runnable {

    private final DataObject nodeData;
    private Context context = null;


    public JSBeautifyRunnable( DataObject nodeData ) {

	this.nodeData = nodeData;
    }

    @Override
    public void run() {

	try {

	    String text;
	    FileObject file = nodeData.getPrimaryFile();
	    DataObject dataObject = null;
	    
	    try {
		dataObject = DataObject.find( file );
	    } catch ( DataObjectNotFoundException ex ) {
		Exceptions.printStackTrace( ex );
	    }

	    if ( dataObject != null ) {

		EditorCookie cEditor = dataObject.getCookie( EditorCookie.class );
		StyledDocument currentDocument = cEditor.getDocument();

		if ( currentDocument != null ) {
		    text = currentDocument.getText( 0, currentDocument.getLength() );
		} else {
		    text = getContent( file );
		}

		Reformat format = Reformat.get( currentDocument );

		format.lock();		

		context = Context.enter();
		context.setLanguageVersion( Context.VERSION_1_6 );
		ScriptableObject scope = context.initStandardObjects();

		Reader reader = new BufferedReader(
			new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "com/drewhamlett/jsbeautify/resources/beautify.js" ),
			Charset.forName( "UTF-8" ) ) );

		context.evaluateReader( scope, reader, "Beautify", 1, null );
		Function fct = ( Function ) scope.get( "js_beautify", scope );
		Object result = fct.call( context, scope, scope, new Object[]{ text } );

		String finalText = result.toString();
		
		currentDocument.remove( 0, currentDocument.getLength() );
		currentDocument.insertString( 0, finalText, null );

		format.unlock();
		cEditor.saveDocument();
		
	    }
	    
	} catch ( Exception ex ) {
	    ErrorManager.getDefault().notify( ErrorManager.WARNING, ex );
	}

    }

    private String getContent( FileObject file ) throws IOException {
	Charset charset = FileEncodingQuery.getEncoding( file );
	return file.asText( charset.name() );
    }
}