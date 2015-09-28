package com.github.fsbarata

public class TestNdkOptions {
	String cFlags;

	Set<String> ldLibs;

	Set<String> abiFilters;

	String getcFlags() {
		return cFlags;
	}

	Set<String> getLdLibs() {
		return ldLibs;
	}

	Set<String> getAbiFilters() {
		return abiFilters;
	}
}
