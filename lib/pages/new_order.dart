import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

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

  final _formKey = GlobalKey<FormState>();

  void saveForm() {
    final form = _formKey.currentState;
    form.validate();
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,

            // Here we take the value from the MyHomePage object that was created by
            // the App.build method, and use it to set our appbar title.
            title: Text("Create Order")),
        body: new SingleChildScrollView(
                      child: Card(
                          child: Padding(
                              padding: EdgeInsets.all(15),
                              child: Form(
                                key: _formKey,
                                child: Column(
                                  children: <Widget>[
                                    TextFormField(
                                      validator: (value) {
                                        if (value.isEmpty) {
                                          return 'Please enter customer name';
                                        }
                                        return null;
                                      },
                                      onSaved: (val) =>
                                      this._customer_name = val,
                                      decoration: InputDecoration(
                                        labelText: "Customer Name",
                                        hintStyle: TextStyle(
                                            fontFamily: "WorkSansSemiBold",
                                            fontSize: 17.0),
                                      ),
                                    ),
                                    Padding(
                                      padding: EdgeInsets.all(5),
                                    ),
                                    TextFormField(
                                      validator: (value) {
                                        if (value.isEmpty) {
                                          return 'Please enter Mobile No';
                                        }
                                        return null;
                                      },
                                      onSaved: (val) => this._mobile = val,
                                      decoration: InputDecoration(
                                        labelText: "Mobile No",
                                        hintStyle: TextStyle(
                                            fontFamily: "WorkSansSemiBold",
                                            fontSize: 17.0),
                                      ),
                                    ),
                                    Padding(
                                      padding: EdgeInsets.all(5),
                                    ),
                                    TextFormField(
                                      onSaved: (val) =>
                                      this._alternative_mobile = val,
                                      decoration: InputDecoration(
                                        labelText: "Alternative Mobile No",
                                        hintStyle: TextStyle(
                                            fontFamily: "WorkSansSemiBold",
                                            fontSize: 17.0),
                                      ),
                                    ),
                                    Padding(
                                      padding: EdgeInsets.all(5),
                                    ),
                                    TextFormField(
                                      onSaved: (val) =>
                                      this._alternative_mobile = val,
                                      decoration: InputDecoration(
                                        labelText: "Address",
                                        hintStyle: TextStyle(
                                            fontFamily: "WorkSansSemiBold",
                                            fontSize: 17.0),
                                      ),
                                    ),
                                    TextFormField(
                                      onSaved: (val) =>
                                      this._alternative_mobile = val,
                                      decoration: InputDecoration(
                                        labelText: "Address",
                                        hintStyle: TextStyle(
                                            fontFamily: "WorkSansSemiBold",
                                            fontSize: 17.0),
                                      ),
                                    ),

                                    MaterialButton(
                                      minWidth: 200.0,
                                      height: 35,
                                      color: Colors.blue,
                                      clipBehavior: Clip.antiAlias,
                                      child: new Text('Create Order',
                                          style: new TextStyle(
                                              fontSize: 16.0,
                                              color: Colors.white)),
                                      onPressed: saveForm,
                                    ),
                                  ],
                                ),
                              ))),),

            );
  }
}
