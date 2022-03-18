#!/usr/bin/env groovy
def call() {
    textWithColor("ENV")
    sh "printenv"
}