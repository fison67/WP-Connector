/**
 *  Wallpad Commax Elevator (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2021 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
*/
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "Wallpad Commax Elevator", namespace: "fison67", author: "fison67") {
        capability "Switch"
	}
}

// parse events into attributes
def parse(String description) {}

def setInfo(data) {
	setPowerOff()
}
def setEventData(data){}

def on(){
	sendCommand(makeCommand(), callback)
    runIn(1, setPowerOff)
}

def off(){
	setPowerOff()
}

def setPowerOff(){
	sendEvent(name:"switch", value:"off")
}


def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        if(jsonObj.result){
        	log.debug "Success to control"
        }else{
        	log.warn "Failed to control"
        }
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(){
	return [
     	"method": "POST",
        "path": "/devices/api/elevatorCall",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ]
    ]
}
