/*
 * Copyright 2014 Sandipan Das.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.reactionlabs.wishconnect.core;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.apache.commons.net.util.Base64;
import org.apache.commons.validator.GenericValidator;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel;

/**
 * @author Sandipan Das
 * @version 1.0
 */
public class Main implements Runnable {

	/**
	 * Constants
	 */
	private static final int NTP_TIMEOUT_DURATION = 500;
	private static final int WISHNET_PORTAL_TIMEOUT_DURATION = 20000;

	private static final String NTP_SERVERS[] = {
		"125.62.193.121", // 2.in.pool.ntp.org
		"192.248.1.168", // 1.asia.pool.ntp.org 
		"27.114.150.12", // 2.asia.pool.ntp.org
	};

	private static final String WISHNET_PORTAL_LOGIN_USERNAME_FORM_ENTITY_NAME = "Username";
	private static final String WISHNET_PORTAL_LOGIN_PASSWORD_FORM_ENTITY_NAME = "Password";
	private static final String WISHNET_PORTAL_EXPIRY_DATE_FORMAT = "hh:mm:ss dd-MM-yyyy";
	private static final String WISHNET_PORTAL_LOGOUT_USERNAME_QUERY_PARAM_NAME = "sessUserName";

	private static final String MATCH_RENEWAL_DATE_TAG_TEXT1 = "Expiry Date";
	private static final String MATCH_RENEWAL_DATE_TAG_TEXT2 = ":";
	private static final String MATCH_RENEWAL_DATE_TAG_TEXT3 = "labelRightPofile1";

	private static final String WISHCONNECT_APPLICATION_TITLE_TEXT = "Wishconnect";
	private static final String WISHCONNECT_PORTAL_LABEL_TEXT = "Portal";
	private static final String WISHCONNECT_USERNAME_LABEL_TEXT = "Username";
	private static final String WISHCONNECT_PASSWORD_LABEL_TEXT = "Password";
	private static final String WISHCONNECT_LOGIN_BUTTON_TEXT = "Login";
	private static final String WISHCONNECT_LOGOUT_BUTTON_TEXT = "Logout";
	private static final String WISHCONNECT_CANCEL_BUTTON_TEXT = "Cancel";

	private static final String WISHCONNECT_LOGIN_DIALOG_BUTTONS_TEXT[] = {
		WISHCONNECT_LOGIN_BUTTON_TEXT, WISHCONNECT_CANCEL_BUTTON_TEXT
	};

	private static final String WISHCONNECT_LOGOUT_DIALOG_BUTTONS_TEXT[] = {
		WISHCONNECT_LOGOUT_BUTTON_TEXT, WISHCONNECT_CANCEL_BUTTON_TEXT
	};

	private static final String WISHCONNECT_INVALID_CONFIG_ERROR_TEXT = "Invalid configuration";
	private static final String WISHCONNECT_EMPTY_FIELDS_ERROR_TEXT = "One or more empty fields";
	private static final String WISHCONNECT_PORTAL_INACCESSIBLE_ERROR_TEXT = "Portal cannot be accessed";
	private static final String WISHCONNECT_INVALID_EXPIRY_DATE_ERROR_TEXT = "Invalid expiry date";
	private static final String WISHCONNECT_ACCESS_DENIED_ERROR_TEXT = "Access denied";

	private static final Color WISHCONNECT_TEXT_VALIDATION_SUCCESS_COLOR = new Color(200, 255, 178);
	private static final Color WISHCONNECT_TEXT_VALIDATION_FAILURE_COLOR = new Color(255, 232, 232);

	private static final String WISHCONNECT_PORTAL_CONFIGURATION_KEY_TEXT = "portal.text";
	private static final String WISHCONNECT_USERNAME_CONFIGURATION_KEY_TEXT = "username.text";
	private static final String WISHCONNECT_PASSWORD_CONFIGURATION_KEY_TEXT = "password.text";
	private static final String WISHCONNECT_CONFIGURATION_FILE_PATH
			= System.getProperty("user.home")
			+ System.getProperty("file.separator")
			+ ".wishconnect-conf";

	private JTextField createUrlValidatedTextField(int columns) {
		JTextField textField = new JTextField(columns) {

			@Override
			public void paintComponent(Graphics g) {
				final int componentWidth = getWidth();
				final int componentHeight = getHeight();
				String sourceText = getText();
				Color backgroundColor = (GenericValidator.isBlankOrNull(sourceText) || !GenericValidator.isUrl(sourceText)) ? WISHCONNECT_TEXT_VALIDATION_FAILURE_COLOR : WISHCONNECT_TEXT_VALIDATION_SUCCESS_COLOR;
				g.setColor(backgroundColor);
				g.fillRect(0, 0, componentWidth, componentHeight);
				super.paintComponent(g);
			}
		};

		textField.setOpaque(false);
		textField.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		return textField;
	}

	private JTextField createBlankOrNullValidatedTextField(int columns) {
		JTextField textField = new JTextField(columns) {

			@Override
			public void paintComponent(Graphics g) {
				final int componentWidth = getWidth();
				final int componentHeight = getHeight();
				String sourceText = getText();
				Color backgroundColor = GenericValidator.isBlankOrNull(sourceText) ? WISHCONNECT_TEXT_VALIDATION_FAILURE_COLOR : WISHCONNECT_TEXT_VALIDATION_SUCCESS_COLOR;
				g.setColor(backgroundColor);
				g.fillRect(0, 0, componentWidth, componentHeight);
				super.paintComponent(g);
			}
		};

		textField.setOpaque(false);
		textField.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		return textField;
	}

	private JPasswordField createBlankOrNullValidatedPasswordField(int columns) {
		JPasswordField passwordField = new JPasswordField(columns) {

			@Override
			public void paintComponent(Graphics g) {
				final int componentWidth = getWidth();
				final int componentHeight = getHeight();
				String sourceText = String.valueOf(getPassword());
				Color backgroundColor = GenericValidator.isBlankOrNull(sourceText) ? WISHCONNECT_TEXT_VALIDATION_FAILURE_COLOR : WISHCONNECT_TEXT_VALIDATION_SUCCESS_COLOR;
				g.setColor(backgroundColor);
				g.fillRect(0, 0, componentWidth, componentHeight);
				super.paintComponent(g);
			}
		};

		passwordField.setOpaque(false);
		passwordField.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		return passwordField;
	}

	private int showAuthenticationDialog(JComponent content, String options[]) {
		int selectedOption = JOptionPane.showOptionDialog(null, content, WISHCONNECT_APPLICATION_TITLE_TEXT, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		return selectedOption;
	}

	private void showErrorDialog(Throwable t) {
		String messageString = ExceptionUtils.getMessage(t);
		String stackTraceString = ExceptionUtils.getStackTrace(t);
		JTextArea exceptionTextArea = new JTextArea(10, 50);
		exceptionTextArea.setOpaque(false);
		exceptionTextArea.setEditable(false);
		exceptionTextArea.setText(stackTraceString);
		exceptionTextArea.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane exceptionScrollPane = new JScrollPane(exceptionTextArea);
		exceptionScrollPane.setBorder(BorderFactory.createEmptyBorder());

		Object[] exceptionObjects = new Object[]{messageString, exceptionScrollPane};
		JOptionPane.showMessageDialog(null, exceptionObjects, WISHCONNECT_APPLICATION_TITLE_TEXT, JOptionPane.ERROR_MESSAGE);
	}

	private void showInformationDialog(JComponent content) {
		JOptionPane.showMessageDialog(null, content, WISHCONNECT_APPLICATION_TITLE_TEXT, JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void run() {
		try {
			JFrame.setDefaultLookAndFeelDecorated(true);
			JDialog.setDefaultLookAndFeelDecorated(true);
			UIManager.put(SubstanceLookAndFeel.WINDOW_ROUNDED_CORNERS, false);
			UIManager.setLookAndFeel(new SubstanceBusinessBlackSteelLookAndFeel());
		} catch (UnsupportedLookAndFeelException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}

		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints gridBagConstraints = new GridBagConstraints();

		JPanel mainPanel = new JPanel();
		JLabel portalLabel = new JLabel(WISHCONNECT_PORTAL_LABEL_TEXT);
		JLabel usernameLabel = new JLabel(WISHCONNECT_USERNAME_LABEL_TEXT);
		JLabel passwordLabel = new JLabel(WISHCONNECT_PASSWORD_LABEL_TEXT);

		JTextField portalTextField = createUrlValidatedTextField(20);
		JTextField usernameTextField = createBlankOrNullValidatedTextField(20);
		JPasswordField passwordTextField = createBlankOrNullValidatedPasswordField(20);

		mainPanel.setLayout(gridBagLayout);
		gridBagConstraints.insets.set(3, 3, 3, 3);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		mainPanel.add(portalLabel, gridBagConstraints);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 1;
		mainPanel.add(usernameLabel, gridBagConstraints);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 1;
		mainPanel.add(passwordLabel, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		mainPanel.add(portalTextField, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 1;
		mainPanel.add(usernameTextField, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 1;
		mainPanel.add(passwordTextField, gridBagConstraints);

		try {
			File wishconnectConfigFile = new File(WISHCONNECT_CONFIGURATION_FILE_PATH);

			if (!wishconnectConfigFile.exists()) {
				wishconnectConfigFile.createNewFile();
			}

			String portal = StringUtils.EMPTY;
			String username = StringUtils.EMPTY;
			String password = StringUtils.EMPTY;
			PropertiesConfiguration wishconnectConfiguration = null;

			try {
				wishconnectConfiguration = new PropertiesConfiguration(wishconnectConfigFile);
				portal = new String(Base64.decodeBase64(wishconnectConfiguration.getString(WISHCONNECT_PORTAL_CONFIGURATION_KEY_TEXT, StringUtils.EMPTY)));
				username = new String(Base64.decodeBase64(wishconnectConfiguration.getString(WISHCONNECT_USERNAME_CONFIGURATION_KEY_TEXT, StringUtils.EMPTY)));
				password = new String(Base64.decodeBase64(wishconnectConfiguration.getString(WISHCONNECT_PASSWORD_CONFIGURATION_KEY_TEXT, StringUtils.EMPTY)));
			} catch (ConfigurationException ex) {
				throw new ConfigurationException(WISHCONNECT_INVALID_CONFIG_ERROR_TEXT, ex);
			}

			portalTextField.setText(portal);
			usernameTextField.setText(username);
			passwordTextField.setText(password);

			NTPUDPClient ntpClient = new NTPUDPClient();
			ntpClient.setDefaultTimeout(NTP_TIMEOUT_DURATION);
			ntpClient.open();

			Iterable<String> ntpServerAddressList = Arrays.asList(NTP_SERVERS);
			Iterator<String> ntpServerAddressListIterator = ntpServerAddressList.iterator();

			while (ntpServerAddressListIterator.hasNext()) {
				try {
					InetAddress ntpInetAddress = InetAddress.getByName(ntpServerAddressListIterator.next());
					ntpClient.getTime(ntpInetAddress);

					if (ntpClient.isOpen()) {
						ntpClient.close();
					}
				} catch (SocketException | SocketTimeoutException ex) {
					// continue and try with the next NTP server
					continue;
				}

				// attempt to logout
				do {
					portalTextField.setEnabled(false);
					usernameTextField.setEnabled(false);
					passwordTextField.setEnabled(false);

					int logoutResult = JOptionPane.showOptionDialog(null, mainPanel, WISHCONNECT_APPLICATION_TITLE_TEXT, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, WISHCONNECT_LOGOUT_DIALOG_BUTTONS_TEXT, WISHCONNECT_LOGOUT_DIALOG_BUTTONS_TEXT[0]);

					if (logoutResult == JOptionPane.OK_OPTION) {
						try (CloseableHttpClient wishnetHttpClient = HttpClients.createDefault()) {

							if (GenericValidator.isBlankOrNull(portal) || !GenericValidator.isUrl(portal) || GenericValidator.isBlankOrNull(username) || GenericValidator.isBlankOrNull(password)) {
								throw new IllegalArgumentException(WISHCONNECT_EMPTY_FIELDS_ERROR_TEXT);
							}

							URI wishnetPortalGenericUrl = URI.create(portal);
							String wishnetPortalUrl = new URL(wishnetPortalGenericUrl.getScheme(), wishnetPortalGenericUrl.getHost(), wishnetPortalGenericUrl.getPort(), StringUtils.EMPTY).toString();
							String wishnetPortalLogoutUrlPath = "/logoutUI.do3";

							URIBuilder wishnetLogoutUriBuilder = new URIBuilder(wishnetPortalUrl).setPath(wishnetPortalLogoutUrlPath).setParameter(WISHNET_PORTAL_LOGOUT_USERNAME_QUERY_PARAM_NAME, usernameTextField.getText());
							RequestConfig wishnetRequestConfig = RequestConfig.copy(RequestConfig.DEFAULT).setSocketTimeout(WISHNET_PORTAL_TIMEOUT_DURATION).build();

							// send the logout HTTP GET request and receive its HTTP response
							HttpGet wishnetLogoutRequest = new HttpGet(wishnetLogoutUriBuilder.build());
							wishnetLogoutRequest.setConfig(wishnetRequestConfig);

							try (CloseableHttpResponse wishnetLogoutResponse = wishnetHttpClient.execute(wishnetLogoutRequest)) {
								int wishnetLogoutResponseStatus = wishnetLogoutResponse.getStatusLine().getStatusCode();
								HttpEntity wishnetLogoutEntity = wishnetLogoutResponse.getEntity();
								EntityUtils.consume(wishnetLogoutEntity);

								if (wishnetLogoutResponseStatus != HttpStatus.SC_OK) {
									continue;
								}
							} catch (ClientProtocolException ex) {
								// need to supress this
							}

						} catch (URISyntaxException | SocketException ex) {
							// if logout fails, let the user try again
							continue;
						}
					}

					// all done
					System.exit(0);

				} while (true);
			}

			// attempt to login
			do {
				portalTextField.setEnabled(true);
				usernameTextField.setEnabled(true);
				passwordTextField.setEnabled(true);

				int loginResult = showAuthenticationDialog(mainPanel, WISHCONNECT_LOGIN_DIALOG_BUTTONS_TEXT);

				if (loginResult == JOptionPane.OK_OPTION) {
					try (CloseableHttpClient wishnetHttpClient = HttpClients.createDefault()) {
						// save currently set username and password
						portal = String.valueOf(portalTextField.getText());
						username = String.valueOf(usernameTextField.getText());
						password = String.valueOf(passwordTextField.getPassword());
						wishconnectConfiguration.setProperty(WISHCONNECT_PORTAL_CONFIGURATION_KEY_TEXT, Base64.encodeBase64String(portal.getBytes()));
						wishconnectConfiguration.setProperty(WISHCONNECT_USERNAME_CONFIGURATION_KEY_TEXT, Base64.encodeBase64String(username.getBytes()));
						wishconnectConfiguration.setProperty(WISHCONNECT_PASSWORD_CONFIGURATION_KEY_TEXT, Base64.encodeBase64String(password.getBytes()));
						wishconnectConfiguration.save();

						if (GenericValidator.isBlankOrNull(portal) || !GenericValidator.isUrl(portal) || GenericValidator.isBlankOrNull(username) || GenericValidator.isBlankOrNull(password)) {
							throw new IllegalArgumentException(WISHCONNECT_EMPTY_FIELDS_ERROR_TEXT);
						}

						URI wishnetPortalGenericUrl = URI.create(portal);
						String wishnetPortalUrl = new URL(wishnetPortalGenericUrl.getScheme(), wishnetPortalGenericUrl.getHost(), wishnetPortalGenericUrl.getPort(), StringUtils.EMPTY).toString();
						String wishnetPortalBasePath = "/" + StringUtils.split(wishnetPortalGenericUrl.getPath(), '/')[0];
						String wishnetPortalIndexUrlPath = wishnetPortalBasePath + "/Login.jsp";
						String wishnetPortalLoginUrlPath = "/loginUI.do2";
						String wishnetPortalProfileUrlPath = wishnetPortalBasePath + "/Profile.jsp";

						// build the index URL to send the HTTP GET request
						URIBuilder wishnetUriBuilder = new URIBuilder(wishnetPortalUrl).setPath(wishnetPortalIndexUrlPath);
						RequestConfig wishnetRequestConfig = RequestConfig.copy(RequestConfig.DEFAULT).setSocketTimeout(WISHNET_PORTAL_TIMEOUT_DURATION).build();

						// send the index HTTP GET request and receive its HTTP response
						HttpGet wishnetIndexRequest = new HttpGet(wishnetUriBuilder.build());
						wishnetIndexRequest.setConfig(wishnetRequestConfig);

						try (CloseableHttpResponse wishnetIndexResponse = wishnetHttpClient.execute(wishnetIndexRequest)) {
							int wishnetIndexResponseStatus = wishnetIndexResponse.getStatusLine().getStatusCode();
							HttpEntity wishnetIndexEntity = wishnetIndexResponse.getEntity();
							EntityUtils.consume(wishnetIndexEntity);

							if (wishnetIndexResponseStatus != HttpStatus.SC_OK) {
								throw new IllegalAccessException(WISHCONNECT_PORTAL_INACCESSIBLE_ERROR_TEXT);
								// continue;
							}
						}

						// build the login URL to send the HTTP POST request
						wishnetUriBuilder = new URIBuilder(wishnetPortalUrl).setPath(wishnetPortalLoginUrlPath);

						// send the login HTTP POST request and receive its HTTP response
						HttpPost wishnetLoginRequest = new HttpPost(wishnetUriBuilder.build());
						List<NameValuePair> wishnetLoginNameValuePairList = new ArrayList();
						wishnetLoginNameValuePairList.add(new BasicNameValuePair(WISHNET_PORTAL_LOGIN_USERNAME_FORM_ENTITY_NAME, username));
						wishnetLoginNameValuePairList.add(new BasicNameValuePair(WISHNET_PORTAL_LOGIN_PASSWORD_FORM_ENTITY_NAME, password));
						wishnetLoginRequest.setEntity(new UrlEncodedFormEntity(wishnetLoginNameValuePairList, Consts.UTF_8));
						wishnetLoginRequest.setConfig(wishnetRequestConfig);

						try (CloseableHttpResponse wishnetLoginResponse = wishnetHttpClient.execute(wishnetLoginRequest)) {
							int wishnetLoginResponseCode = wishnetLoginResponse.getStatusLine().getStatusCode();
							HttpEntity wishnetLoginEntity = wishnetLoginResponse.getEntity();
							EntityUtils.consume(wishnetLoginEntity);

							if (wishnetLoginResponseCode != HttpStatus.SC_MOVED_TEMPORARILY) {
								continue;
							}
						}

						// build the profile URL to send the HTTP GET request
						wishnetUriBuilder = new URIBuilder(wishnetPortalUrl).setPath(wishnetPortalProfileUrlPath);

						// send the profile HTTP GET request and receive its HTTP response
						HttpGet wishnetProfileRequest = new HttpGet(wishnetUriBuilder.build());
						wishnetProfileRequest.setConfig(wishnetRequestConfig);

						String wishnetProfileContent = StringUtils.EMPTY;

						try (CloseableHttpResponse wishnetProfileResponse = wishnetHttpClient.execute(wishnetProfileRequest)) {
							int wishnetProfileResponseStatus = wishnetProfileResponse.getStatusLine().getStatusCode();
							HttpEntity wishnetProfileEntity = wishnetProfileResponse.getEntity();
							wishnetProfileContent = EntityUtils.toString(wishnetProfileEntity);
							EntityUtils.consume(wishnetProfileEntity);

							if (wishnetProfileResponseStatus != HttpStatus.SC_OK) {
								continue;
							}

							if (StringUtils.isBlank(wishnetProfileContent)) {
								throw new IllegalAccessException(WISHCONNECT_ACCESS_DENIED_ERROR_TEXT);
							}

							ntpServerAddressListIterator = ntpServerAddressList.iterator();
							ntpClient.open();

							while (ntpServerAddressListIterator.hasNext()) {
								try {
									InetAddress ntpInetAddress = InetAddress.getByName(ntpServerAddressListIterator.next());
									TimeInfo ntpTimeInfo = ntpClient.getTime(ntpInetAddress);
									NtpV3Packet ntpPacket = ntpTimeInfo.getMessage();
									TimeStamp ntpTimeStamp = ntpPacket.getReferenceTimeStamp();
									long currentDateMillis = ntpTimeStamp.getTime();
									ntpClient.close();

									Document responseDocument = Jsoup.parse(wishnetProfileContent);
									Elements nodes = responseDocument.getElementsByTag("td");
									Iterator<Element> nodeIterator = nodes.iterator();

									String expiryDateString = StringUtils.EMPTY;

									while (nodeIterator.hasNext()) {
										Element currentNode = nodeIterator.next();
										String currentNodeText = currentNode.text();

										if (currentNodeText.equals(MATCH_RENEWAL_DATE_TAG_TEXT1) && nodeIterator.hasNext()) {
											currentNode = nodeIterator.next();
											currentNodeText = currentNode.text();

											if (currentNodeText.equals(MATCH_RENEWAL_DATE_TAG_TEXT2) && nodeIterator.hasNext()) {
												currentNode = nodeIterator.next();
												currentNodeText = currentNode.text();

												if (currentNode.attr("class").equals(MATCH_RENEWAL_DATE_TAG_TEXT3) && !StringUtils.isBlank(currentNodeText)) {
													// extract the expiry date from the profile page
													expiryDateString = currentNodeText;
												}
											}
										}
									}

									if (GenericValidator.isBlankOrNull(expiryDateString)) {
										throw new IllegalAccessException(WISHCONNECT_ACCESS_DENIED_ERROR_TEXT);
									}

									// append additionals
									expiryDateString = "23:59:59 " + expiryDateString;

									// parse the expiry date
									SimpleDateFormat wishnetExpiryDateFormat = new SimpleDateFormat(WISHNET_PORTAL_EXPIRY_DATE_FORMAT);
									Date expiryDate = wishnetExpiryDateFormat.parse(expiryDateString);

									// store back the well-formatted date
									expiryDateString = expiryDate.toString();

									long millisDifference = expiryDate.getTime() - currentDateMillis;
									long secondsDifference = (millisDifference / 1000);  // convert millis to seconds
									long minutesDifference = (secondsDifference / 60);   // convert seconds to minutes
									long hoursDifference = (minutesDifference / 60);     // convert minutes to hours
									long daysDifference = (hoursDifference / 24);        // convert hours to days

									String timeRemainingString
											= String.format("%1$d days %2$d hours %3$d minutes %4$d seconds",
													daysDifference,
													hoursDifference % 24,
													minutesDifference % 60,
													secondsDifference % 60);

									JPanel expiryDatePanel = new JPanel();
									LayoutManager expiryDatePanelLayoutManager = new BoxLayout(expiryDatePanel, BoxLayout.Y_AXIS);
									expiryDatePanel.setLayout(expiryDatePanelLayoutManager);
									expiryDatePanel.add(new JLabel(expiryDateString, SwingConstants.LEFT));
									expiryDatePanel.add(new JLabel(timeRemainingString, SwingConstants.LEFT));

									// show status information
									showInformationDialog(expiryDatePanel);

									// all done
									System.exit(0);

								} catch (SocketException | SocketTimeoutException ex) {
									// time to try with the next NTP server
									continue;
								} catch (ParseException ex) {
									// the expiry date was either not found or was not in the expected format
									throw new IllegalStateException(WISHCONNECT_INVALID_EXPIRY_DATE_ERROR_TEXT, ex);
								}
							}

							// if none of the NTP servers are still accessible, its because the login was unsuccessful
							throw new IllegalAccessException(WISHCONNECT_ACCESS_DENIED_ERROR_TEXT);
						}

					} catch (URISyntaxException | IOException | IllegalAccessException | IllegalArgumentException | IllegalStateException ex) {
						// show the error details
						showErrorDialog(ex);

						// if login fails, let the user try again
						continue;
					}
				}

				// all done
				System.exit(0);

			} while (true);

		} catch (IOException | ConfigurationException ex) {
			// show the error details
			showErrorDialog(ex);
		}
	}

	public static void main(String[] args) {
		Runnable application = new Main();
		SwingUtilities.invokeLater(application);
	}
}
