import 'dart:convert';

import 'package:flutter/material.dart';
import 'dart:collection';

import 'package:flutter/services.dart';
import 'package:flutter_page_indicator/flutter_page_indicator.dart';
import 'package:flutter_swiper/flutter_swiper.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:video_player/video_player.dart';

import 'model/album.dart';
import 'model/media_item.dart';

// import 'package:cabinroadphotos2/components/slideshow_app_bar.dart';
// import 'package:cabinroadphotos2/photos_library_api/album.dart';
// import 'package:cabinroadphotos2/util/read_more_text.dart';
// import 'package:flutter/material.dart';
// import 'package:flutter/services.dart';
// import 'package:flutter_page_indicator/flutter_page_indicator.dart';
// import 'package:flutter_swiper/flutter_swiper.dart';
// import 'package:photo_view/photo_view.dart';
// import 'package:cabinroadphotos2/photos_library_api/media_item.dart';
// import 'package:video_player/video_player.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or press Run > Flutter Hot Reload in a Flutter IDE). Notice that the
        // counter didn't reset back to zero; the application is not restarted.
        primarySwatch: Colors.blue,
      ),
      debugShowCheckedModeBanner: false,
      home: SlideshowPage(),
    );
  }
}

// class MyHomePage extends StatefulWidget {
//   MyHomePage({Key key, this.title}) : super(key: key);
//
//   // This widget is the home page of your application. It is stateful, meaning
//   // that it has a State object (defined below) that contains fields that affect
//   // how it looks.
//
//   // This class is the configuration for the state. It holds the values (in this
//   // case the title) provided by the parent (in this case the App widget) and
//   // used by the build method of the State. Fields in a Widget subclass are
//   // always marked "final".
//
//   final String title;
//
//   @override
//   _MyHomePageState createState() => _MyHomePageState();
// }
//
// class _MyHomePageState extends State<MyHomePage> {
//   int _counter = 0;
//   String text;
//   static const platform = const MethodChannel('slideshowChannel/test');
//
//   String _responseFromNativeCode = "not set yet";
//
//   void _incrementCounter() {
//     setState(() {
//       // This call to setState tells the Flutter framework that something has
//       // changed in this State, which causes it to rerun the build method below
//       // so that the display can reflect the updated values. If we changed
//       // _counter without calling setState(), then the build method would not be
//       // called again, and so nothing would appear to happen.
//       _counter++;
//     });
//   }
//
//
//
//   @override
//   Widget build(BuildContext context) {
//     // This method is rerun every time setState is called, for instance as done
//     // by the _incrementCounter method above.
//     //
//     // The Flutter framework has been optimized to make rerunning build methods
//     // fast, so that you can just rebuild anything that needs updating rather
//     // than having to individually change instances of widgets.
//     return Scaffold(
//       body: Center(
//         // Center is a layout widget. It takes a single child and positions it
//         // in the middle of the parent.
//         child: Column(
//           // Column is also a layout widget. It takes a list of children and
//           // arranges them vertically. By default, it sizes itself to fit its
//           // children horizontally, and tries to be as tall as its parent.
//           //
//           // Invoke "debug painting" (press "p" in the console, choose the
//           // "Toggle Debug Paint" action from the Flutter Inspector in Android
//           // Studio, or the "Toggle Debug Paint" command in Visual Studio Code)
//           // to see the wireframe for each widget.
//           //
//           // Column has various properties to control how it sizes itself and
//           // how it positions its children. Here we use mainAxisAlignment to
//           // center the children vertically; the main axis here is the vertical
//           // axis because Columns are vertical (the cross axis would be
//           // horizontal).
//           mainAxisAlignment: MainAxisAlignment.center,
//           children: <Widget>[
//             Text(
//               'You have pushed the button this many times:',
//             ),
//             Text(
//               _responseFromNativeCode,
//               style: Theme.of(context).textTheme.headline4,
//             ),
//           ],
//         ),
//       ),
//       floatingActionButton: FloatingActionButton(
//         onPressed: getNativeResponse,
//         tooltip: 'Increment',
//         child: Icon(Icons.add),
//       ), // This trailing comma makes auto-formatting nicer for build methods.
//     );
//   }
// }

class SlideshowPage extends StatefulWidget {
  const SlideshowPage({Key key})
      : super(key: key);
  @override
  State<StatefulWidget> createState() =>
      _SlideshowPageState();
}
//
class _SlideshowPageState extends State<SlideshowPage> {
  bool showControls = false;

  _SlideshowPageState();
  static const platform = const MethodChannel('slideshowChannel/mediaItems');

  int index = 0;
  int autoplayDuration;
  bool autoplay;
  Future<Album> album;
  List<MediaItem> mediaItems;
  // bool showControls = false;
  // bool isSlideshow;

  SwiperController _controller;

  @override
  void dispose() {
    // SystemChrome.setEnabledSystemUIOverlays(SystemUiOverlay.values);
    super.dispose();
  }

  @override
  initState() {

    SystemChrome.setEnabledSystemUIOverlays([]);
    _controller = new SwiperController();

    getPreferences();

    album = getNativeData();

    WidgetsBinding.instance
        .addPostFrameCallback((_) => _controller.move(index, animation: false));
    super.initState();
  }

  Future<Album> getNativeData() async {
    String response = "";
    try {
      final String result = await platform.invokeMethod('getMediaItems');
      response = result;
      print("got " + response);
    } on PlatformException catch (e) {
      response = "Failed to Invoke: '${e.message}'.";
    }
    Map mediaMap = jsonDecode(response);
    return Album.fromJson(mediaMap);
  }

  @override
  Widget build(BuildContext context) {
    // print("build: " + showControls.toString());
    return Scaffold(
        backgroundColor: Colors.black87,
        extendBodyBehindAppBar: true,
        body: Stack(
          children: <Widget>[
            Scaffold(
                extendBodyBehindAppBar: true,
                backgroundColor: Colors.transparent,
                body: FutureBuilder<Album>(
                  future: album,
                  builder: _buildSlideshow,
                )
            )
          ],
        ));
  }

  Widget _buildImageTile(BuildContext context, int index) {
    // print(albumQueue.elementAt(index).baseUrl);
    MediaItem mediaItem = mediaItems.elementAt(index);
    if(mediaItem.type == "photo") {
      return new Image.network(
        mediaItems.elementAt(index).baseUrl,
        fit: BoxFit.fitHeight,
      );
    } else {
      print(mediaItems.elementAt(index).baseUrl + "=dv");
      // TODO - cache/download videos for playing
      return new VideoApp(
        autoplay: autoplay,
        controller: new VideoPlayerController.network(mediaItems.elementAt(index).baseUrl + "=dv"),
      );
    }

  }

  void _toggleControls(int num) {
    // print("tapped");
    setState((){
      showControls = !showControls;
    });
  }

  Widget _buildSlideshow(BuildContext context, AsyncSnapshot<Album> snapshot) {
    if (snapshot.hasData) {
      if (snapshot.data.mediaItems == null) {
        return Container();
      }
      mediaItems = snapshot.data.mediaItems;
      return Swiper(
        itemBuilder: (BuildContext context, int index) {
          return _buildImageTile(context, index);
          // print(albumQueue.elementAt(index).baseUrl);
          // return new Image.network(
          //   albumQueue.elementAt(index).baseUrl,
          //   fit: BoxFit.fitHeight,
          // );
        },
        indicatorLayout: PageIndicatorLayout.COLOR,
        autoplay: true,
        autoplayDelay: autoplayDuration * 1000,
        itemCount: snapshot.data.mediaItems.length,
        // pagination: _pagination,
        control: new SwiperControl(
          iconPrevious: showControls ? Icons.arrow_back_ios : null,
          iconNext: showControls ? Icons.arrow_forward_ios : null,
        ),
        controller: _controller,
        onTap: _toggleControls,
      );
    }

    return Center(
      child: const CircularProgressIndicator(),
    );
  }

  void getPreferences() async {
    SharedPreferences pref = await SharedPreferences.getInstance();
    if (pref.getInt("autoplaySpeed") == null) {
      pref.setInt("autoplaySpeed", 20);
    }
    autoplayDuration = pref.getInt("autoplaySpeed");

    if (pref.getBool("autoplay") == null) {
      pref.setBool("autoplay", false);
    }
    autoplay = pref.getBool("autoplay");
  }
}



class VideoApp extends StatefulWidget {
  @required final bool autoplay;
  @required final VideoPlayerController controller;

  const VideoApp({Key key, this.autoplay, this.controller}) : super(key: key);

  @override
  _VideoAppState createState() => _VideoAppState(controller: controller);
}

class _VideoAppState extends State<VideoApp> {
  _VideoAppState({this.controller});
  VideoPlayerController controller;
  // String url;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Video Demo',
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: Colors.transparent,
        body: Center(
          child: controller.value.initialized
              ? AspectRatio(
            aspectRatio: controller.value.aspectRatio,
            child: VideoPlayer(controller),
          )
              : Container(),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            setState(() {
              controller.value.isPlaying
                  ? controller.pause()
                  : controller.play();
            });
          },
          backgroundColor: Colors.green[800],
          child: Icon(
            controller.value.isPlaying ? Icons.pause : Icons.play_arrow,

          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
    controller.dispose();
  }
}