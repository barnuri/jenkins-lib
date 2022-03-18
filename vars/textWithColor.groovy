#!/usr/bin/env groovy
def call(text, color = 'blue') {
    ansiColor('xterm') {
        colorCode = "\033[${backgroundTextColorCode(color)}m"
        resetCode = "\033[0m"
        echo "${colorCode} --------------------------------------- ${resetCode}"
        echo "${colorCode} ${text} ${resetCode}"
        echo "${colorCode} --------------------------------------- ${resetCode}"
    }
}

def backgroundTextColorCode(color = 'blue') {
    color = color.toLowerCase()
    if(color == 'black') {
        return "40"
    } else if(color == 'red') {
        return "41"
    } else if(color == 'green') {
        return "42"
    } else if(color == 'orange') {
        return "43"
    } else if(color == 'magenta') {
        return "45"
    } else if(color == 'cyan') {
        return "46"
    } else if(color == 'cyan') {
        return "47"
    }
    return "44"
}
