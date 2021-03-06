# spotify-connect-router
A small web server that saves custom names for Spotify Connect devices to make it easier to change them using voice commands (it also handles authenticating with the Spotify API and refreshing the token).

It was created to allow switching the playback device using a Google Home with IFTTT webhooks.

## Prerequisites
* Spotify premium
* Server with a static IP and Java 8 installed (e.g. Rapsberry Pi)
* (For IFTTT webhooks) The server has to be internet facing and needs one port forwarded

## Setup instructions
### Groundwork
There is a lot of stuff to set up (like ports and credentials)
* Login to https://beta.developer.spotify.com/dashboard/ - you will need to create an app in your profile to get app credentials for authorizing API calls
  * Create an app
  * No, you're not developing a commercial application (unless you do, then click yes)
  * Fill in a name, description, check the boxes, click "Create"
  * Now you have an app with a Client ID and Client Secret, we will need those in a second
* Download the latest JAR and application.properties into a folder on your server
* Edit the application.properties
  * Set server.port to whatever port you want to
  * Set client_id to the Client ID from the app in the Spotify dashboard
  * Set client_secret to the Client Secret from the app in the Spotify dashboard
  * Set redirect_url to your_static_server_address:server.port/redirect
    * The address can be local but you have to be able to open it from a web browser
* Click on "Edit settings" in the Spotify dashboard
  * Add the exact same address to Redirect URIs that you entered in the application.properties
  * Don't forget to save
### Web server
We need to authorize our app to use our Spotify account and we will set up some nice names for our devices
* Start the jar (java -jar spotify-connect-router-x.x.x.jar)
  * On startup a text file named spotify_connect_router_UUID will be created
  * You will need its content for all calls to your server to make sure no one who randomly found the open endpoint can mess with your spotify playback
  * If you need a new one just delete the file and restart the server
* Open the address of your server:port in a web browser
  * You should see an ugly web page with an input field and a log in link
  * Input the content of the spotify_connect_router_UUID file into the input field (and click "Save cookies" if you don't want to reinput it again)
  * Click "Login to spotify"
  * You will be asked to log in to your Spotify account and to authenticate the app. Do both if you want to proceed.
  * The next page should say "Logged in successfully". If it doesn't you probably did something wrong and  have to start over. Sorry
* Go to the setup page
  * Here you should see all currently available Spotify Connect devices
  * Also stuff like your PC or Smartphone should show up if you have the Spotify app running
    * If you don't see any entries make sure that the UUID is correct (take it from the file spotify_connect_router_UUID on your server)
  * Give your devices easy and distinguishable names (use the save button for every name you give)
  * Test it:
    * Manually start the playback on any device you see in the list (e.g. your PC)
    * Use the play button next to a different devices to transfer the playback there
    * This has to work before doing any IFTTT integration!
### Google Home + IFTTT integration
Now we want to enable Google Home to transfer the Spotify Playback to different Spotify Connect devices
* Login to https://ifttt.com/ (or create a new account)
* Click on "My Applets"
* Click on "New Applet"
  * Click on "this"
    * Search for Google Assistant
    * If prompted authorize the connection between IFTTT and Google Assistant
    * Choose "Say a phrase with a text ingredient"
    * Enter a phrase with "$" as the variable
      * For me it works best if the variable is the last thing, e.g. "Spotify on $"
    * Write a response (useful to also output the variable here to see if Google understood your correctly)
    * Set your language for the voice command
    * Click "Create trigger"
  * Click on "that"
    * Search for Webhooks
    * Choose "Make a web request"
    * Input the correct url:
      * The global url to your server + the port you chose
      * /transferPlayback?uuid=
      * the content of the spotify_connect_router_UUID file
    * Method: PUT
    * Content Type: application/json
    * Body: {"alias": " {{TextField}}"}
    * Don't forget to save
  * Now you should be able to use your new voice trigger together with the names you set up in the web interface to transfer Spotify playback to different devices
    * "Hey Google, play Spotify"
    * "Hey Google, Spotify on Receiver" (e.g.)
  * You can create a different applet to use the "/pause" endpoint to pause the Spotify playback
  
## Endpoints
All endpoints except for "/redirect" need the uuid generated by the server as request param (e.g. http://localhost:9999/devices?uuid=xx-xx-xx-xx-xx). If the Spotify API returns a 401 (unauthorized) every endpoint except for /login and /redirect will try to refresh the API token once.
* /login
  * GET
    * Builds the URL to Spotify's authorization endpoint and redirects there. Should only be used via browser, not via REST client
* /redirect
  * GET
    * Used to get the code to obtain the API token. Only used as a redirect from the Spotify authorization endpoint, redirects to the success page. Should not be called manually.
* /devices
  * GET
    * Queries the https://api.spotify.com/v1/me/player/devices endpoint. Returns the currently available Spotify devices
* /alias
  * GET
    * Returns a list of [mappings from device id to the name set by the user](https://github.com/drusin/spotify-connect-router/blob/master/src/main/java/dawid/connect_router/AliasDeviceMapping.java).
  * PUT
    * Creates a new mapping or updates an existing one. Needs an [AliasDeviceMapping](https://github.com/drusin/spotify-connect-router/blob/master/src/main/java/dawid/connect_router/AliasDeviceMapping.java) in the body.
* /transferPlayback
  * PUT
    * Transfers the playback to the device with the provided user set name. Needs an [Alias](https://github.com/drusin/spotify-connect-router/blob/master/src/main/java/dawid/connect_router/Alias.java) in the body.
* /pause
  * PUT
    * Pauses the Spotify playback.
* /play
  * PUT
    * Resumes the Spotify playback.
* /next
  * PUT
    * Skip playback to next track.
* /previous
  * PUT
    * Skip playback to previous track.
    
## I want to play around with the code. It doesn't compile! Where are the getters and setters?
This project uses [Lombok](https://projectlombok.org/). There are installation instructions for most popular IDEs on their nice webpage.

To start the application from source just use "gradlew bootRun".
