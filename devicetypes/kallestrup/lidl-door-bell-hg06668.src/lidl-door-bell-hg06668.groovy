/**
 *  Lidl Door Bell
 *
 *  Copyright 2021 Jack Kallestrup
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType
 
metadata {
	definition (name: "Lidl Door Bell HG06668", namespace: "Kallestrup", author: "Jack Kallestrup", runLocally: true, , executeCommandsLocally: true) {    
		capability "Button"
        capability "Battery"
        capability "Refresh"
        
        fingerprint profileId: "0402", inClusters: "0000,0001,0003,0500,0b05", outClusters: "0019", 
        	manufacturer: "_TZ1800_ladpngdx", model: "TS0211", deviceJoinName: "Silvercrest Smart Door Bell", mnmn: "SmartThings", vid: "Zigbee Non-Holdable Button"
	}

	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#FFFFFF"
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }        
		main (["button"])
		details(["button", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	    
    def event = zigbee.getEvent(description)
    log.debug "Event " + event
    
    if (event) {
    	log.debug event
    	sendEvent(event)
    } else {
    	if (description?.startsWith('zone status')) {
            event = parseIasButtonMessage(description)
            log.debug "zone status: " + event
        } else {
	    	log.debug "no event"
    	    def zigbeeMap = zigbee.parseDescriptionAsMap(description)
    	    if (zigbeeMap) {
        		log.debug zigbeeMap
	        } else {
    	    	log.debug "no Map"
	        }
        }
    }
    log.debug "Parse returned $event"
    
    return event ? createEvent(event) : []
}

private Map parseIasButtonMessage(String description) {
    log.debug description
    return getButtonResult("pushed")
}

def refresh() {
    log.debug "Refreshing"
    
    log.debug "0000: " + zigbee.readAttribute(0x0000,0x00)
    log.debug "0001: " + zigbee.readAttribute(0x0001,0x00)
    log.debug "0003: " + zigbee.readAttribute(0x0003,0x00)
    log.debug "0500: " + zigbee.readAttribute(0x0500,0x00)
    log.debug "0b05: " + zigbee.readAttribute(0x0b05,0x00)
    log.debug "enrol: " + zigbee.enrollResponse()
    log.debug "power: "  + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20)
    return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20) + zigbee.enrollResponse()
}

def installed() {
	log.debug "Installer"
    initialize()
    sendEvent(name: "supportedButtonValues", value: ["pushed"], displayed: false)
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], displayed: false)
}

def configure() {
	log.debug "Configuring Reporting ${device.getDataValue("model")}"
	def cmds = []
    return  zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20) +
            cmds
}

def updated() {
	initialize()
}

def initialize() {
	// These devices don't report regularly so they should only go OFFLINE when Hub is OFFLINE
	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)
}

private Map getButtonResult(buttonState) {
	def descriptionText = "$device.displayName was $buttonState"
    //return createEvent(name: "button", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    return createEvent(name: "button", value: buttonState, data: [buttonState], descriptionText: descriptionText, isStateChange: true)
}