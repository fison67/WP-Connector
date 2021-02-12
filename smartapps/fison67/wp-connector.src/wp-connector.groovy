/**
 *  WP Connector (v.0.0.1)
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
import groovy.json.JsonOutput
import groovy.transform.Field

definition(
    name: "WP Connector",
    namespace: "fison67",
    author: "fison67",
    description: "A Connector between Wallpad and ST",
    category: "My Apps",
    iconUrl: "https://i.pinimg.com/originals/d3/2f/98/d32f98a07286c51fcc6a34080a3bec7e.jpg",
    iconX2Url: "https://i.pinimg.com/originals/d3/2f/98/d32f98a07286c51fcc6a34080a3bec7e.jpg",
    iconX3Url: "https://i.pinimg.com/originals/d3/2f/98/d32f98a07286c51fcc6a34080a3bec7e.jpg",
    oauth: true
)

preferences {
   page(name: "mainPage")
   page(name: "monitorPage")
}


def mainPage() {
	if(location.hubs.size() < 1) {
        section() {
            paragraph "[ERROR]\nSmartThings Hub not found.\nYou need a SmartThings Hub to use HA connector."
        }
        return
    }
    
	dynamicPage(name: "mainPage", title: "Botem Connector", nextPage: null, uninstall: true, install: true) {
   		section("Request New Devices"){
        	input "address", "text", title: "Server address", required: true, description:"IP:Port. ex)192.168.0.100:30300"
        	href url:"http://${settings.address}", style:"embedded", required:false, title:"Local Management", description:"This makes you easy to setup"
            input "devHub", "enum", title: "Hub", required: true, multiple: false, options: getHubs()
        }
        
       	section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
       	}
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    
    if (!state.accessToken) {
        createAccessToken()
    }
    
	state.dniHeaderStr = "wp-connector-"
    
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    initialize()
}

/**
* deviceNetworkID : Reference Device. Not Remote Device
*/
def getDeviceToNotifyList(deviceNetworkID){
	def list = []
	state.monitorMap.each{ targetNetworkID, _data -> 
        if(deviceNetworkID == _data.id){
        	def item = [:]
            item['id'] = state.dniHeaderStr + targetNetworkID
            item['data'] = _data.data
            list.push(item)
        }
    }
    return list
}

def initialize() {
	log.debug "initialize"
    
    def options = [
     	"method": "POST",
        "path": "/settings/api/smartthings",
        "headers": [
        	"HOST": settings.address,
            "Content-Type": "application/json"
        ],
        "body":[
            "app_url":"${apiServerUrl}/api/smartapps/installations/",
            "app_id":app.id,
            "access_token":state.accessToken
        ]
    ]
    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: null])
    sendHubCommand(myhubAction)
}

def dataCallback(physicalgraph.device.HubResponse hubResponse) {
    def msg, json, status
    try {
        msg = parseLanMessage(hubResponse.description)
        status = msg.status
        json = msg.json
        log.debug "${json}"
    } catch (e) {
        logger('warn', "Exception caught while parsing data: "+e);
    }
}

def getDataList(){
    def options = [
     	"method": "GET",
        "path": "/requestDevice",
        "headers": [
        	"HOST": settings.address,
            "Content-Type": "application/json"
        ]
    ]
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: dataCallback])
    sendHubCommand(myhubAction)
}

def addDevice(){
	def data = request.JSON
    log.debug data
    
    def result = [result: 'success']
    
    if(data.type == "commax"){
    	result = processAddCommaxDevice(data)
    }else{
    	result = [result: 'unkown']
    }
    
    def resultString = new groovy.json.JsonOutput().toJson(result)
    render contentType: "application/javascript", data: resultString
}

def processAddCommaxDevice(data){
	def dni = state.dniHeaderStr + "commax-" + data.id.toLowerCase()
    def dth = null
    def namespace = "fison67"
    
    def rootDevice 		= data.data.rootDevice
    def commaxDevice 	= data.data.commaxDevice
	if(rootDevice == "switch"){
    	if(commaxDevice == "standbyPowerSwitch"){
        	dth = "Wallpad Commax Wall Socket"
        }else if(commaxDevice == "light"){
        	dth = "Wallpad Commax Light"
        }else if(commaxDevice == "awaySwitch"){
        	dth = "Wallpad Commax Away Switch"
        }else if(commaxDevice == "preventSwitch"){
        	dth = "Wallpad Commax Prevent Switch"
       	}else if(commaxDevice == "mainSwitch"){
        	dth = "Wallpad Commax Main Switch"
        }else if(commaxDevice == "fanSystem"){
        	namespace = "streamorange58819"
        	dth = "Wallpad Commax Fan"
        }
    }else if(rootDevice == "thermostat"){
    	if(commaxDevice == "boiler"){
        	dth = "Wallpad Commax Boiler"
        }
    }else if(rootDevice == "detectSensors"){
    	if(commaxDevice == "contactSensor"){
        	dth = "Wallpad Commax Contact Sensor"
        }
    }else if(rootDevice == "lock"){
    	if(commaxDevice == "gasLock"){
        	dth = "Wallpad Commax Gas Lock"
        }
    }
    
    if(!_existDevice(dni)){
    	try{
        	_registerDevice(namespace, dni, dth, data)
            return [result: 'success']
        }catch(err){
        	return [result: 'failed', etc: "${err}"]
        }
    }else{
    	result = [result: 'exist']
    }
}

def _registerDevice(namespace, dni, dth, data){
	def childDevice = addChildDevice(namespace, dth, dni, getHubID(devHub), [
        "label": data.name
    ])  
    childDevice.setInfo(data)
}

def _existDevice(dni){
	def existDevice = false;
    list.each { child ->
        def _dni = child.deviceNetworkId
        if(_dni == dni){
        	existDevice = true
        }
    }
    return existDevice
}

def updateDevice(){
	def data = request.JSON
    log.debug data
    def id = data.id
    def dni = state.dniHeaderStr + data.type + "-" + id.toLowerCase()
    def chlid = getChildDevice(dni)
    if(chlid){
		chlid.setEventData(data.data)
    }
    def resultString = new groovy.json.JsonOutput().toJson("result":true)
    render contentType: "application/javascript", data: resultString
}

def getDeviceList(){
	def list = getChildDevices();
    def resultList = [];
    list.each { child ->
        def dni = child.deviceNetworkId
        resultList.push( dni.substring(13, dni.length()) );
    }
    
    def configString = new groovy.json.JsonOutput().toJson("list":resultList)
    render contentType: "application/javascript", data: configString
}

def authError() {
    [error: "Permission denied"]
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "Botem Connector API",
        platforms: [
            [
                platform: "SmartThings Tuya Connector",
                name: "Botem Connector",
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app.id,
                access_token:  state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def _getServerURL(){
     return settings.address
}

def getHubs(){
	def list = []
    location.getHubs().each { hub ->
    	list.push(hub.name)
    }
    return list
}

def getHubID(name){
	def id = null
    location.getHubs().each { hub ->
    	if(hub.name == name){
        	id = hub.id
        }
    }
    return id
}


mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/config")                         { action: [GET: "authError"] }
        path("/list")                         	{ action: [GET: "authError"]  }
        path("/update")                         { action: [POST: "authError"]  }
        path("/add")                         	{ action: [POST: "authError"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/list")                         	{ action: [GET: "getDeviceList"]  }
        path("/update")                         { action: [POST: "updateDevice"]  }
        path("/add")                         	{ action: [POST: "addDevice"]  }
    }
}
