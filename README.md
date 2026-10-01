# DuablSubPlayer

Android application for watching videos with subtitles and learning languages through contextual translation.

DuablSubPlayer allows users to watch local videos or YouTube videos, load subtitles, translate them while watching, save words to a personal dictionary, and practice vocabulary through a built-in training game.

## Features

* Local video playback with `.srt` subtitles
* YouTube video support
* Automatic subtitle retrieval for YouTube videos
* Synchronized subtitles during video playback
* Translation of subtitles into the selected language
* Automatic language identification
* Interactive subtitle text with word selection
* Personal vocabulary dictionary
* Saved words grouped by video
* Vocabulary training game
* Saved video list
* Swipe-to-delete videos
* Separate subtitle reader
* Multiple interface languages
* Light and dark UI themes

## Technologies

* **Kotlin**
* **Android SDK**
* **AndroidX**
* **Material Components**
* **Media3 / ExoPlayer** — local video playback
* **YouTube Android Player** — YouTube playback
* **Google ML Kit** — language identification and translation
* **Room** — vocabulary database
* **Gson** — local video list storage
* **OkHttp** — network requests
* **YouTube Transcript API** — subtitle retrieval
* **Glide** — image loading
* **Gradle Kotlin DSL**
* **KSP**

## How It Works

### Local videos

1. Select a video from the device.
2. Select an `.srt` subtitle file.
3. Open the video player.
4. Subtitles are synchronized with the video.
5. Select words from the subtitles to translate or save them to the dictionary.

### YouTube videos

1. Paste a YouTube video link.
2. The application retrieves the video title and thumbnail.
3. Available subtitles are downloaded automatically.
4. The subtitles are converted to `.srt` format and stored locally.
5. The video can then be opened with synchronized subtitles.

### Vocabulary learning

Words selected from subtitles can be saved to the local dictionary.

The dictionary stores:

* original word or phrase
* translation
* video it came from
* number of correct answers during training

Saved vocabulary can then be used in the built-in multiple-choice training game.

## Project Structure

```text
src/main/java/com/example/dualsubplayer/
├── adapter/
├── data/
│   ├── AppDatabase.kt
│   ├── DictionaryDao.kt
│   ├── DictionaryWord.kt
│   └── VideoStorageManager.kt
├── network/
│   ├── OkHttpYoutubeClient.kt
│   └── YoutubeSubtitleFetcher.kt
├── ui/
│   ├── DictionaryFragment.kt
│   ├── DictionaryWordsFragment.kt
│   ├── GameFragment.kt
│   ├── HomeFragment.kt
│   ├── MainActivity.kt
│   ├── PlayerActivity.kt
│   ├── SettingsFragment.kt
│   ├── SubtitleReaderActivity.kt
│   └── TranslationsFragment.kt
└── utils/
    └── SrtParser.kt
```

## Local Storage

The application keeps its data locally on the device.

* **Room** stores saved vocabulary.
* **SharedPreferences** stores the saved video list.
* Downloaded YouTube subtitles are stored in the application's cache.

No external database or user account is required.

## Supported Languages

The application interface includes:

* English
* Ukrainian
* French
* German
* Spanish
* Polish

Subtitle translation is handled through Google ML Kit language models.

## Requirements

* Android 7.0 (API 24) or newer
* Internet connection for YouTube content, subtitle retrieval, and downloading translation language packs

## Running Locally

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/KatyaYatcenko/DuablSubPlayer.git
```

Then:

1. Open the project in Android Studio.
2. Allow Gradle to download the required dependencies.
3. Connect an Android device or start an emulator.
4. Build and run the application.

## Possible Improvements

* Improve subtitle parsing for additional subtitle formats
* Add more vocabulary training modes
* Improve offline translation management
* Add playback progress and learning statistics
* Add support for additional video and subtitle sources
