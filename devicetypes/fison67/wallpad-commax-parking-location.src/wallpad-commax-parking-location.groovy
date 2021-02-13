/**
 *  Wallpad Commax Parking Location (v.0.0.1)
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
	definition (name: "Wallpad Commax Parking Location", namespace: "fison67", author: "fison67") {
        capability "Sensor"
        capability "Refresh"
        
        attribute "total", "string"
        attribute "locations", "string"
	}
}

// parse events into attributes
def parse(String description) {}

def setInfo(data) {}
def setEventData(data){}

def refresh(){
	log.debug "refresh"
    sendCommand(makeCommand(), callback)
}


def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
        if(jsonObj.result){
        	log.debug jsonObj.result
            
            def total = ""
        	def locations = ""
            def list = jsonObj.result
            for ( item in list ) {
                total += (item.time + "에 위치 " + item.location + "에 주차되었습니다. ")
                if(locations != ""){
                	locations += ", "
                }
                locations += (item.location)
            }
            sendEvent(name: "total", value: total)
            sendEvent(name: "locations", value: locations)
        }else{
        	log.warn "Failed"
        }
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {}

def sendCommand(options, _callback){
	log.debug options
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(){
	return [
     	"method": "POST",
        "path": "/devices/api/parkingLocation",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ]
    ]
}
