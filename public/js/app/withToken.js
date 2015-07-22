// call this function with the base URL of with
// e.g "http://localhost:9000

function requestToken( doWithToken, withBase ) {
	 var withWindow = window.open( withBase+"/assets/index.html#login" );
	 var answered = false;
	 function receiveToken( event ) {
		 var regexp = /Token: (.+)/;
		 var data = event.data;
		 var match = regexp.exec( data );
		 if( match != undefined ) {
			 doWithToken( match[1] );
		 } else {
			 doWithToken( undefined );
		 }
		 answered = true;
		 clearInterval( regPostRequest );
		 withWindow.close();
		 window.removeEventListener( "message", receiveToken );
	 }
	 
	 
	 window.addEventListener( "message", receiveToken, false );
	 // send request for token every second
	 var regPostRequest = setInterval( function() {
		 withWindow.postMessage( "requestToken","*");		 
	 }, 1000 );
	 

	 
	 // on timeout remove the event listener and if nothing was sent yet
	 // maybe send undefined 
	 setTimeout( 
		function() { 
			window.removeEventListener( "message", receiveToken );
			clearInterval( regPostRequest );
			if( !answered ) { doWithToken( undefined);} 
		}, 60000 );	 
 }
 
