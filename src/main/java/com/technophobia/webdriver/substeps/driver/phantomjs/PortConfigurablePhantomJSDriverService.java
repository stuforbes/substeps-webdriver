/*
 *	Copyright Technophobia Ltd 2012
 *
 *   This file is part of Substeps.
 *
 *    Substeps is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Substeps is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with Substeps.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.technophobia.webdriver.substeps.driver.phantomjs;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.browserlaunchers.Proxies;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.os.CommandLine;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.phantomjs.PhantomJSDriverService.Builder;

/** 
 * Workaround class to deal with the issue that {@link PhantomJSDriverService} does not allow the port
 * to be configurable
 * @author sforbes
 *
 */
public class PortConfigurablePhantomJSDriverService {

	static final String PHANTOMJS_DEFAULT_EXECUTABLE = "phantomjs";
	private static final String PHANTOMJS_DEFAULT_LOGFILE = "phantomjsdriver.log";

	/**
	 * Configures and returns a new {@link PhantomJSDriverService} using the
	 * default configuration.
	 * <p/>
	 * In this configuration, the service will use the PhantomJS executable
	 * identified by the the following capability, system property or PATH
	 * environment variables:
	 * <ul>
	 * <li>{@link PhantomJSDriverService#PHANTOMJS_EXECUTABLE_PATH_PROPERTY}</li>
	 * <li>
	 * {@link PhantomJSDriverService#PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY}
	 * (Optional - without will use GhostDriver internal to PhantomJS)</li>
	 * </ul>
	 * <p/>
	 * Each service created by this method will be configured to find and use a
	 * free port on the current system.
	 * 
	 * @return A new ChromeDriverService using the default configuration.
	 */
	public static PhantomJSDriverService createServiceForPort(
			final int port, final Capabilities desiredCapabilities) {
		// Look for Proxy configuration within the Capabilities
		Proxy proxy = null;
		if (desiredCapabilities != null) {
			proxy = Proxies.extractProxy(desiredCapabilities);
		}

		// Find PhantomJS executable
		File phantomjsfile = findPhantomJS(desiredCapabilities);

		// Find GhostDriver main JavaScript file
		File ghostDriverfile = findGhostDriver(desiredCapabilities);

		// Find command line arguments to add
		String[] commandLineArguments = findCommandLineArguments(desiredCapabilities);

		// Build & return service
		return new Builder().usingPhantomJSExecutable(phantomjsfile) //
				.usingGhostDriver(ghostDriverfile) //
				.usingPort(port) //
				.withProxy(proxy) //
				.withLogFile(new File(PHANTOMJS_DEFAULT_LOGFILE)) //
				.usingCommandLineArguments(commandLineArguments) //
				.build();
	}

	/**
	 * Looks into the Capabilities, the current $PATH and the System Properties
	 * for {@link PhantomJSDriverService#PHANTOMJS_EXECUTABLE_PATH_PROPERTY}.
	 * <p/>
	 * NOTE: If the Capability, the $PATH and the System Property are set, the
	 * Capability takes priority over the System Property, that in turn takes
	 * priority over the $PATH.
	 * 
	 * @param desiredCapabilities
	 *            Capabilities in which we will look for the path to PhantomJS
	 * @param docsLink
	 *            The link to the PhantomJS documentation page
	 * @param downloadLink
	 *            The link to the PhantomJS download page
	 * @return The driver executable as a {@link File} object
	 * @throws IllegalStateException
	 *             If the executable not found or cannot be executed
	 */
	@SuppressWarnings("deprecation")
	protected static File findPhantomJS(Capabilities desiredCapabilities) {
		String phantomjspath = null;
		if (desiredCapabilities != null
				&& desiredCapabilities
						.getCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY) != null) {
			phantomjspath = (String) desiredCapabilities
					.getCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY);
		} else {
			phantomjspath = CommandLine.find(PHANTOMJS_DEFAULT_EXECUTABLE);
			phantomjspath = System.getProperty(
					PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
					phantomjspath);
		}

		checkState(
				phantomjspath != null,
				"The path to the driver executable must be set by the %s capability/system property/PATH variable;",
				PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY);

		File phantomjs = new File(phantomjspath);

		checkExecutable(phantomjs);
		return phantomjs;
	}

	/**
	 * Find the GhostDriver main file (i.e. <code>"main.js"</code>).
	 * <p/>
	 * Looks into the Capabilities and the System Properties for
	 * {@link PhantomJSDriverService#PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY}.
	 * <p/>
	 * NOTE: If both the Capability and the System Property are set, the
	 * Capability takes priority.
	 * 
	 * @param desiredCapabilities
	 *            Capabilities in which we will look for the path to GhostDriver
	 * @param docsLink
	 *            The link to the GhostDriver documentation page
	 * @param downloadLink
	 *            The link to the GhostDriver download page
	 * @return The driver executable as a {@link File} object
	 * @throws IllegalStateException
	 *             If the executable not found or cannot be executed
	 */
	protected static File findGhostDriver(Capabilities desiredCapabilities) {
		// Recover path to GhostDriver from the System Properties or the
		// Capabilities
		String ghostdriverpath = null;
		if (desiredCapabilities != null
				&& (String) desiredCapabilities
						.getCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY) != null) {
			ghostdriverpath = (String) desiredCapabilities
					.getCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY);
		} else {
			ghostdriverpath = System.getProperty(
					PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY,
					ghostdriverpath);
		}

		if (ghostdriverpath != null) {
			checkState(
					ghostdriverpath != null,
					"The path to the driver executable must be set by the '%s' capability/system property;",
					PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY);

			// Check few things on the file before returning it
			File ghostdriver = new File(ghostdriverpath);
			checkState(ghostdriver.exists(),
					"The GhostDriver does not exist: %s",
					ghostdriver.getAbsolutePath());
			checkState(ghostdriver.isFile(),
					"The GhostDriver is a directory: %s",
					ghostdriver.getAbsolutePath());
			checkState(ghostdriver.canRead(),
					"The GhostDriver is not a readable file: %s",
					ghostdriver.getAbsolutePath());

			return ghostdriver;
		}

		// This means that no GhostDriver System Property nor Capability was set
		return null;
	}

	protected static void checkExecutable(File exe) {
		checkState(exe.exists(), "The driver executable does not exist: %s",
				exe.getAbsolutePath());
		checkState(!exe.isDirectory(),
				"The driver executable is a directory: %s",
				exe.getAbsolutePath());
		checkState(FileHandler.canExecute(exe),
				"The driver is not executable: %s", exe.getAbsolutePath());
	}

	private static String[] findCommandLineArguments(
			Capabilities desiredCapabilities) {
		String[] args = new String[] {};
		if (desiredCapabilities != null) {
			Object capability = desiredCapabilities
					.getCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS);
			if (capability != null) {
				args = (String[]) capability;
			}
		}
		return args;
	}
}
