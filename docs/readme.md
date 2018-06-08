# Generate Code Documentation
## Steps

### Automatic
		
* Open terminal navigate to SDK root directory.
* In SDK root directory, execute **./gradlew build**
* This will generate HTML documentation in **android-sdk/build/docs/javadoc** directory
* Browse **core-api/build/docs/javadoc/index.html** in browser.

### Manual for whole project
		
* Open project in Android Studio.
* Go to **Tools -> Generate JavaDoc**
* Make sure **Generate JavaDoc scope -> Whole project** is checked.
* Uncheck **Include test sources**.
* Select **Output Directory**.
* Click **OK**.
* This will generate HTML documentation in given **Output Directory**.
* Browse **Output Directory/index.html** in browser.
