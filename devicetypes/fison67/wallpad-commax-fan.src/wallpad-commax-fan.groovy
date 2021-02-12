/**
 *  Wallpad Commax Fan (v.0.0.1)
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
	definition (name: "Wallpad Commax Fan", namespace: "streamorange58819", author: "fison67", mnmn: "fison67", vid: "ce63cfcf-6aed-3fe7-8177-64571ac94642") {
        capability "Switch"
        capability "streamorange58819.fanspeed"
	}
}

// parse events into attributes
def parse(String description) {}

def setInfo(data) {
    state.id = data.id
    state.data = data.toString()
    for (subDevice in data.data.subDevice) {
       if(subDevice.sort == "switchBinary"){
    		sendEvent(name: "switch", value: subDevice.value)
       }else if(subDevice.sort == "fanSpeed"){
    		sendEvent(name: "fanspeed", value: subDevice.value)
       }
    }
}
def setEventData(data){
	log.debug "Event >> ${data}"
    for (target in data) {
        def subDevice = getSubDeviceById(target.subUuid)
        if(subDevice){
        	if(subDevice.sort == "switchBinary"){
    			sendEvent(name: "switch", value: target.value)
            }else if(subDevice.sort == "fanSpeed"){
    			sendEvent(name: "fanspeed", value: target.value)
            }
        }
    }
}

def on(){
	control(makeSwitchCommand("on"))
}

def off(){
	control(makeSwitchCommand("off"))
}

def low(){
	control(makeSwitchCommand("low"))
}

def medium(){
	control(makeFanSpeedCommand("medium"))
}

def high(){
	control(makeFanSpeedCommand("high"))
}

def setFanSpeed(speed){
	control(makeFanSpeedCommand(speed))
}

def makeSwitchCommand(value){
	def data = getSubDevice("switchBinary")
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

def makeFanSpeedCommand(value){
	def data = getSubDevice("fanSpeed")
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
