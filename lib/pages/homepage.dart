import 'package:bubble_bottom_bar/bubble_bottom_bar.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:glinic_manager/pages/new_order.dart';
import 'package:glinic_manager/services/authentication.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

final FirebaseMessaging _firebaseMessaging = FirebaseMessaging();

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.auth, this.userId, this.logoutCallback})
      : super(key: key);

  final BaseAuth auth;
  final VoidCallback logoutCallback;
  final String userId;




  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int currentIndex=0;
  String _batteryPercentage = 'Battery precentage';
  List<dynamic> deviceList = [];

  signOut() async {
    try {
      await widget.auth.signOut();
      widget.logoutCallback();
    } catch (e) {
      print(e);
    }
  }

  static const batteryChannel = const MethodChannel('MyNativeChannel');
  static const eventchannel = EventChannel('MyEventChannel');


  Future<void> initPlatformState() async {



  }

  createeventChannel(){

    eventchannel.receiveBroadcastStream().listen((dynamic event) {
      if(event == "PEN_AUTHORIZED"){
        print("Here our PEN_AUTHORIZED");
        Navigator.of(context).pushReplacement(PageRouteBuilder(pageBuilder: (_,__,___)=> new CreateOrderPage()));

      }
    }, onError: (dynamic error) {
      print('Received error: ${error.message}');
    });



   
  }



  Future<void> _connectDevice(String id) async {

    try {
      String res = await batteryChannel.invokeMethod('connectDevice',{"id":id});
      print(deviceList);

    } on PlatformException catch (e) {
      print("Failed to get battery level: '${e.message}'.");
    }


    print(id);
}
  Future<void> _getDeviceList() async {
    List<dynamic> dceList = [];

    try {
       dceList = await batteryChannel.invokeMethod('getDeviceList');
      print(deviceList);

    } on PlatformException catch (e) {
      print("Failed to get battery level: '${e.message}'.");
    }

    setState(() {
      deviceList = dceList;
    });


  }




  Future<void> _initPen() async {
    String batteryPercentage;
    try {
      String res = await batteryChannel.invokeMethod('initPen');
      print(res);

    } on PlatformException catch (e) {
      batteryPercentage = "Failed to get battery level: '${e.message}'.";
    }

    setState(() {
      _batteryPercentage = "batteryPercentage";
    });
  }

  @override
  void initState() {
    super.initState();
    _firebaseMessaging.configure(
      onMessage: (Map<String, dynamic> message) async {
        print("onMessage: $message");
//        _showItemDialog(message);
      },
      onLaunch: (Map<String, dynamic> message) async {
        print("onLaunch: $message");
//        _navigateToItemDetail(message);
      },
      onResume: (Map<String, dynamic> message) async {
        print("onResume: $message");
//        _navigateToItemDetail(message);
      },
    );
    _firebaseMessaging.requestNotificationPermissions(
        const IosNotificationSettings(
            sound: true, badge: true, alert: true, provisional: true));
    _firebaseMessaging.onIosSettingsRegistered
        .listen((IosNotificationSettings settings) {
      print("Settings registered: $settings");
    });
    _firebaseMessaging.getToken().then((String token) {
      assert(token != null);
      setState(() {
//        _homeScreenText = "Push Messaging token: $token";
      });

    });

    _initPen();
    createeventChannel();
  }

  void changePage(int index){
    setState(() {
      currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      drawer: Drawer(
        child: Column(
          // Important: Remove any padding from the ListView.
          mainAxisSize: MainAxisSize.max,
          children: <Widget>[
            DrawerHeader(
              child: Text('Drawer Header'),
              decoration: BoxDecoration(
                color: Colors.blue,
              ),
            ),
            ListTile(
              title: Text('Item 1'),
              onTap: () {
                // Update the state of the app
                // ...
                // Then close the drawer
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: Text('Item 2'),
              onTap: () {
                // Update the state of the app
                // ...
                // Then close the drawer
                Navigator.pop(context);
              },
            ),
            new Expanded(
              child: new Align(
                alignment: Alignment.bottomCenter,
                child:

                new ListTile(

                  leading: Icon(Icons.power_settings_new),
                  title: Text('Logout'),
                  onTap: () {
                    signOut();
                    Navigator.pop(context);
                  },
                ),
              ),
            ),
          ],
        ),
      ),

      appBar: AppBar(
        centerTitle: true,

        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text("Glinic"),
        actions: <Widget>[
          IconButton(

              onPressed: (){},
              icon:Icon(Icons.notifications)
          )
        ],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.endDocked,
      bottomNavigationBar: BubbleBottomBar(
        fabLocation: BubbleBottomBarFabLocation.end,
        hasNotch: true,
        hasInk: true,
        opacity: .2,
        currentIndex: currentIndex,
        onTap: changePage,
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        elevation: 8,
        items: <BubbleBottomBarItem>[
          BubbleBottomBarItem(backgroundColor: Colors.red, icon: Icon(Icons.dashboard, color: Colors.black,), activeIcon: Icon(Icons.dashboard, color: Colors.red,), title: Text("Home")),
          BubbleBottomBarItem(backgroundColor: Colors.deepPurple, icon: Icon(Icons.access_time, color: Colors.black,), activeIcon: Icon(Icons.access_time, color: Colors.deepPurple,), title: Text("Logs")),
          BubbleBottomBarItem(backgroundColor: Colors.indigo, icon: Icon(Icons.folder_open, color: Colors.black,), activeIcon: Icon(Icons.folder_open, color: Colors.indigo,), title: Text("Folders")),
          BubbleBottomBarItem(backgroundColor: Colors.green, icon: Icon(Icons.menu, color: Colors.black,), activeIcon: Icon(Icons.menu, color: Colors.green,), title: Text("Menu"))
        ],
      ),

      body: Container(
        padding: EdgeInsets.all(50),
        // Center is a layout widget. It takes a single child and positions it
        // in the middle of the parent.
          child:deviceList.length>0? ListView.builder(
            
            itemCount: deviceList.length,
              itemBuilder: (context, i) {

              return MaterialButton(
                onPressed: (){
                  _connectDevice(deviceList[i].keys.first);
                },
                child: Text(deviceList[i].values.first),
              );

              }
          ):Text("No devices")
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _getDeviceList,
        tooltip: 'Increment',
        child: Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}

