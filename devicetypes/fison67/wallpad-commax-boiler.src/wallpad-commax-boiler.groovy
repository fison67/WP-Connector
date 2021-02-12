/**
 *  Wallpad Commax Boiler (v.0.0.1)
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
	definition (name: "Wallpad Commax Boiler", namespace: "fison67", author: "fison67", ocfDeviceType: "oic.d.thermostat") {
        capability "Thermostat Mode"
        capability "Thermostat Heating Setpoint"
        capability "Temperature Measurement"
	}
}

// parse events into attributes
def parse(String description) {}

def setInfo(data) {
    state.id = data.id
    state.data = data.toString()
    for (subDevice in data.data.subDevice) {
       if(subDevice.sort == "thermostatMode"){
    		sendEvent(name: "supportedThermostatModes", value: subDevice.subOption)
    		sendEvent(name: "thermostatMode", value: subDevice.value)
       }else if(subDevice.sort == "airTemperature"){
    		sendEvent(name: "temperature", value: subDevice.value as int, unit: subDevice.scale[0])
       }else if(subDevice.sort == "thermostatSetpoint"){
    		sendEvent(name: "heatingSetpoint", value: subDevice.value as int, unit: subDevice.scale[0])
       }
    }
}
def setEventData(data){
	log.debug "Event >> ${data}"
    for (target in data) {
        def subDevice = getSubDeviceById(target.subUuid)
        if(subDevice){
        	if(subDevice.sort == "thermostatMode"){
    			sendEvent(name: "thermostatMode", value: target.value)
            }else if(subDevice.sort == "airTemperature"){
    			sendEvent(name: "temperature", value: target.value as int, unit: subDevice.scale[0])
            }else if(subDevice.sort == "thermostatSetpoint"){
    			sendEvent(name: "heatingSetpoint", value: target.value as int, unit: subDevice.scale[0])
            }
        }
    }
}

def setHeatingSetpoint(temperature){
	control(makeHeatingSetPointCommand(temperature))
}

def setThermostatMode(mode){
	control(makeModeCommand(mode))
}

def heat(){
	control(makeModeCommand("heat"))
}

def off(){
	control(makeModeCommand("off"))
}

def makeHeatingSetPointCommand(value){
	def data = getSubDevice("thermostatSetpoint")
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

def makeModeCommand(value){
	def data = getSubDevice("thermostatSetpoint")
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
