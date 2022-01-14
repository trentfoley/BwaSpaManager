/*
 *  BWA Spa
 *
 *  Copyright 2022 Trent Foley
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

metadata {
    definition (name: "BWA Spa", namespace: "trentfoley", author: "Trent Foley") {
        capability "Temperature Measurement"
        capability "Thermostat Heating Setpoint"
        capability "Thermostat Mode"
        capability "Thermostat Operating State"
        capability "Refresh"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "temperature", type: "generic", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState "temperature", label: '${currentValue}°', unit:"dF", action: "toggleSwitch", canChangeIcon: true, defaultState: true, backgroundColors: [
                	[value: 80, color: "#153591"],
            		[value: 84, color: "#1e9cbb"],
            		[value: 88, color: "#90d2a7"],
            		[value: 92, color: "#44b621"],
            		[value: 96, color: "#f1d801"],
            		[value: 100, color: "#d04e00"],
            		[value: 104, color: "#bc2323"]
        		]
            }
        }
        
        controlTile("heatSliderControl", "device.heatingSetpoint", "slider", width: 2, height: 1, inactiveLabel: false, range: getTemperatureRange()) {
			state "heatingSetpoint", action:"setHeatingSetpoint", backgroundColor: "#d04e00"
		}
        
        standardTile("thermostatModeControl", "device.thermostatMode", width: 2, height: 1, decoration: "flat") {
            state "schedule" , label: 'Rest', action: "toggleThermostatMode", icon: "st.Bedroom.bedroom2", defaultState: true
            state "auto", label: 'Ready', action: "toggleThermostatMode", icon: "st.Health & Wellness.health2"
        }

        standardTile("refresh", "device.refresh", width: 2, height: 1, decoration: "flat") {
            state "refresh", label: 'refresh', action: "refresh", icon: "st.secondary.refresh", defaultState: true
        }

        main("temperature")

        details(["temperature", "heatSliderControl", "thermostatModeControl", "refresh"])
    }
}

def getTemperatureRange() {
	"(26.5..104)"
}

// called from parent when initially setup
def initialize() {
	log.debug "initialize()"
    sendEvent(name: "supportedThermostatModes", value: ["auto", "schedule"], displayed: false)
    refresh()
    runEvery15Minutes(refresh)
}

// parse events into attributes
def parse(String description) {
    log.debug "parse('${description}')"
}

// Implementation of capability.refresh
def refresh() {
    log.debug "refresh()"
    def data = parent.pollChild(this)

    if(data) {
        sendEvent(name: "temperature", value: data.temperature, unit: data.temperatureScale)
        sendEvent(name: "heatingSetpoint", value: data.heatingSetpoint, unit: data.temperatureScale)
        sendEvent(name: "thermostatMode", value: data.thermostatMode)
        sendEvent(name: "thermostatOperatingState", value: data.thermostatOperatingState)
    } else {
        log.error "ERROR: Device connection removed? No data found for ${device.deviceNetworkId} after polling"
    }
}

// Implementation of capability.thermostatHeatingSetpoint
def setHeatingSetpoint(degreesF) {
    log.debug "setHeatingSetpoint(${degreesF})"
    parent.setHeatingSetpoint(this, degreesF)
    sendEvent(name: "heatingSetpoint", value: degreesF, unit: "F")
}

// Implementation of capability.thermostatMode
// Valid values are: "auto" and "schedule"
def setThermostatMode(String mode) {
    log.debug "setThermostatMode(${mode})"
    parent.setThermostatMode(this, mode)
    sendEvent(name: "thermostatMode", value: mode)
}

// Implementation of capability.thermostatMode
def auto() { setThermostatMode("auto") }

def toggleThermostatMode() {
    def currentMode = device.currentValue("thermostatMode")
    def newMode = (currentMode == "schedule") ? "auto" : "schedule"
    setThermostatMode(newMode)
}