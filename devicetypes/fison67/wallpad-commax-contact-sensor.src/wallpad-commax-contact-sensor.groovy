/**
 *  Wallpad Commax Contact Sensor (v.0.0.1)
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
	definition (name: "Wallpad Commax Contact Sensor", namespace: "fison67", author: "fison67", ocfDeviceType: "x.com.st.d.sensor.contact") {
        capability "Contact Sensor"
	}
}

// parse events into attributes
def parse(String description) {}

def setInfo(data) {
    state.id = data.id
    state.data = data.toString()
    for (subDevice in data.data.subDevice) {
       if(subDevice.sort == "generalPurpose"){
    		sendEvent(name: "contact", value: subDevice.value == "undetected" ? "closed" : "open")
       }
    }
}
def setEventData(data){
	log.debug "Event >> ${data}"
    for (target in data) {
        def subDevice = getSubDeviceById(target.subUuid)
        if(subDevice){
        	if(subDevice.sort == "generalPurpose"){
    			sendEvent(name: "contact", value: target.value == "undetected" ? "closed" : "open")
            }
        }
    }
}

def updated() {}
