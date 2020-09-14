import 'dart:ui';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:zoom_widget/zoom_widget.dart';

class CreateOrderPage extends StatefulWidget {
  @override
  _CreateOrderState createState() => new _CreateOrderState();
}

class _CreateOrderState extends State<CreateOrderPage> {
  String _customer_name;
  String _mobile;
  String _alternative_mobile;
  bool dropDown = false;
  String email = "rishav00a@gmail.com";
  static const batteryChannel = const MethodChannel('MyNativeChannel');
  static const eventchannel = EventChannel('MyEventChannel');
  final _formKey = GlobalKey<FormState>();
  List<Offset> points=[];

  List<dynamic> strokeData = [];
  void saveForm() {
    final form = _formKey.currentState;
    form.validate();
  }

  createeventChannel(){

    eventchannel.receiveBroadcastStream().listen((dynamic event) {
      print("reponse from channel");
        print(event);
      strokeData = event.split(",");
      print(strokeData.toString());

//      *5.22
//      *3.7
      Offset point = new Offset(double.parse(strokeData[3].split("=")[1]), double.parse(strokeData[4].split("=")[1]));

      print(point);
      setState(() {
        points.add(point);
      });


    }, onError: (dynamic error) {
      print('Received error: ${error.message}');
    });

  }


  Future<void> _saveFile() async {

    try {
      String res = await batteryChannel.invokeMethod('saveFile');
      print(res);


    } on PlatformException catch (e) {
      print("Error: '${e.message}'.");
    }

  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    createeventChannel();
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,

            // Here we take the value from the MyHomePage object that was created by
            // the App.build method, and use it to set our appbar title.
            title: Text("My Notes")),
        body: new Container(
    color: Colors.white,

    child: Container(

      height: MediaQuery.of(context).size.height,
      width: MediaQuery.of(context).size.width,

    color: Colors.yellow,
    child: CustomPaint(
        size: Size(91, 117),

                      painter: FaceOutlinePainter(

                          points
                      )),




                      ),)


    );
  }
}

class FaceOutlinePainter extends CustomPainter {
  final List<Offset> points;
  FaceOutlinePainter(this.points);

  @override
  void paint(Canvas canvas, Size size) {


//    canvas.translate(size., dy)
    // Define a paint object
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0
      ..color = Colors.indigo
      ..strokeCap = StrokeCap.round;



//     canvas.scale(91,117);


    canvas.drawPoints(PointMode.points, points, paint);
  }

  @override
  bool shouldRepaint(FaceOutlinePainter oldDelegate) => true;
}