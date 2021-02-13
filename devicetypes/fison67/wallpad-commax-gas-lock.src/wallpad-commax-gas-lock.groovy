/**
 *  Wallpad Commax Gas Lock (v.0.0.1)
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
	definition (name: "Wallpad Commax Gas Lock", namespace: "fison67", author: "fison67", ocfDeviceType: "oic.d.smartlock") {
        capability "Lock"
	}

}

// parse events into attributes
def parse(String description) {}

def setInfo(data) {
    state.id = data.id
    state.data = data.toString()
    for (subDevice in data.data.subDevice) {
       if(subDevice.sort == "gasLock"){
    		sendEvent(name: "lock", value: subDevice.value == "lock" ? "locked" : "unlocked")
       }
    }
}

def setEventData(data){
	log.debug "Event >> ${data}"
    for (target in data) {
        def subDevice = getSubDeviceById(target.subUuid)
        if(subDevice){
        	if(subDevice.sort == "gasLock"){
    			sendEvent(name: "lock", value: target.value == "lock" ? "locked" : "unlocked")
            }
        }
    }
}

def lock(){
	control(makeLockCommand("lock"))
}

def unlock(){
	control(makeLockCommand("unlock"))
}

def makeLockCommand(value){
	def data = getSubDevice("gasLock")
	return [
      "rootUuid": state.id,
      "subDevice": [
         "funcCommand": "set",
         "sort": data.sort,
         "subUuid": data.subUuid,
         "type": data.type,
         "value": value
      ]
    ]
}

def getSubDevice(sort){
	def result
	def data = new JsonSlurper().parseText(state.data)
    for (subDevice in data.data.subDevice) {
		if(subDevice.sort == sort){
        	result = subDevice
        }
	}
    return result
}

def getSubDeviceById(uuid){
	def result
	def data = new JsonSlurper().parseText(state.data)
    for (subDevice in data.data.subDevice) {
		if(subDevice.subUuid == uuid){
        	result = subDevice
        }
	}
    return result
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

def control(data){
	sendCommand(makeCommand(data), callback)
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/devices/api/control",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}
