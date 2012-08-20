package com.drewhamlett.jsbeautify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.mozilla.javascript.*;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID( category = "Build",
id = "com.drewhamlett.jsbeautify.JSBeautify" )
@ActionRegistration( displayName = "#CTL_JSBeautify" )
@ActionReferences( {
	@ActionReference( path = "Menu/Source", position = 200 ),
	@ActionReference( path = "Loaders/text/javascript/Actions", position = 0 ),
	@ActionReference( path = "Editors/text/javascript/Popup", position = 400, separatorAfter = 450 ),
	@ActionReference( path = "Editors/text/x-json/Popup", position = 400, separatorAfter = 450 )
} )
@Messages( "CTL_JSBeautify=JSBeautify" )
public final class JSBeautify implements ActionListener {

	private final DataObject dataObject;
	private final String beautifyPath = "com/drewhamlett/jsbeautify/resources/beautify.js";

	public JSBeautify( DataObject dataObject ) {
		this.dataObject = dataObject;
	}

	@Override
	public void actionPerformed( ActionEvent ev ) {

		try {

			FileObject file = dataObject.getPrimaryFile();
			format( file );

		} catch (BadLocationException ex) {
			Exceptions.printStackTrace( ex );
		}
	}

	public void format( FileObject file ) throws BadLocationException {

		try {

			EditorCookie ec = dataObject.getCookie( EditorCookie.class );

			final StyledDocument doc = ec.openDocument();
			final Reformat reformat = Reformat.get( doc );
			final String unformattedText = doc.getText( 0, doc.getLength() );
			final JEditorPane[] openedPanes = ec.getOpenedPanes();

			reformat.lock();

			try {

				NbDocument.runAtomic( doc, new Runnable() {
					@Override
					public void run() {

						try {

							reformat.reformat( 0, doc.getLength() );
							Context context = Context.enter();
							context.setLanguageVersion( Context.VERSION_1_6 );
							ScriptableObject scope = context.initStandardObjects();

							Reader reader = new BufferedReader( new InputStreamReader(
									getClass().getClassLoader().getResourceAsStream( beautifyPath ),
									Charset.forName( "UTF-8" ) ) );

							context.evaluateReader( scope, reader, "Beautify", 1, null );
							Function fct = ( Function ) scope.get( "js_beautify", scope );

							boolean preserveNewLines = JSBeautifyOptions.getInstance().getOption( "preserveNewLines", true );
							boolean useTabs = JSBeautifyOptions.getInstance().getOption( "useTabs", false );
							boolean spaceBeforeConditional = JSBeautifyOptions.getInstance().getOption( "spaceBeforeConditional", true );
							boolean jslintHappy = JSBeautifyOptions.getInstance().getOption( "jslintHappy", false );
							boolean indentCase = JSBeautifyOptions.getInstance().getOption( "indentCase", false );
							int indentSize = JSBeautifyOptions.getInstance().getOption( "indentSize", 1 );
							String braceStyle = JSBeautifyOptions.getInstance().getOption( "braceStyle", "collapse" );

							NativeObject properties = new NativeObject();

							if ( useTabs ) {
								properties.defineProperty( "indent_char", "\t", NativeObject.READONLY );
								properties.defineProperty( "indent_size", 1, NativeObject.READONLY );
							} else {
								int size = 4;
								if ( indentSize == 0 ) {
									size = 2;
								} else if ( indentSize == 1 ) {
									size = 4;
								} else {
									size = 8;
								}
								properties.defineProperty( "indent_size", size, NativeObject.READONLY );
							}

							properties.defineProperty( "preserve_newlines", preserveNewLines, NativeObject.READONLY );
							properties.defineProperty( "max_preserve_newlines", false, NativeObject.READONLY );
							properties.defineProperty( "jslint_happy", jslintHappy, NativeObject.READONLY );
							properties.defineProperty( "space_before_conditional", spaceBeforeConditional, NativeObject.READONLY );
							properties.defineProperty( "indent_case", indentCase, NativeObject.READONLY );

							properties.defineProperty( "brace_style", braceStyle, NativeObject.READONLY );


							Object result = fct.call( context, scope, scope, new Object[]{ unformattedText, properties } );

							String finalText = result.toString();
							int pos = openedPanes[0].getCaretPosition();

							doc.remove( 0, doc.getLength() );
							doc.insertString( 0, finalText, null );

							try {
								openedPanes[0].setCaretPosition( pos );
							} catch (Exception e) {
								openedPanes[0].setCaretPosition( doc.getLength() );
							}

						} catch (BadLocationException ex) {
							Exceptions.printStackTrace( ex );
						} catch (IOException ex) {
							Exceptions.printStackTrace( ex );
						}
					}
				} );

			} finally {
				reformat.unlock();
			}

			ec.saveDocument();

		} catch (IOException ex) {
			Exceptions.printStackTrace( ex );
		} finally {
		}
	}
}
