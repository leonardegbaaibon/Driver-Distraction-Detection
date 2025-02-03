import React, { useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
} from "react-native";
import LargeInput from "../components/Input/LargeInput";
import { LoginApi } from "../utils/api/apis/LoginApi";
import AsyncStorage from "@react-native-async-storage/async-storage";
import LoadingModal from "../components/Modal/LoadingModal";
import DeviceInfo from "react-native-device-info";
import { NativeModules } from "react-native"; // Import NativeModules


// Define styles directly in the component file
const styles = {
  textInput: {
    fontSize: 15,
    borderRadius: 5,
    padding: 10,
    marginBottom: 10,
    backgroundColor: '#292E41',
    fontFamily: "JuliusSansOneRegular",
  },
  calendarInput: {
    fontSize: 15,
    borderRadius: 5,
    paddingVertical: 10,
    width: '80%',
    backgroundColor: '#292E41'
  },
  calenderInputContainer: {
    borderRadius: 5,
    marginBottom: 20,
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: '#292E41',
  },
  button: {
    backgroundColor: '#EEE8E0',
    borderRadius: 5,
    padding: 12,
    alignItems: 'center',
    width: '100%',
    marginBottom: 30,
    marginTop: 10
  },
  buttonText: {
    color: '#E9B962',
    fontSize: 18,
    fontFamily: "JuliusSansOneRegular",
  },
  titleText: {
    color: 'white',
    fontSize: 23,
    marginHorizontal: 25,
    fontFamily: "JuliusSansOneRegular",
  }
};

const LoginScreen = ({ navigation }) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isModalVisible, setIsModalVisible] = useState(false);

  const validateEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const handleLogin = async () => {
    setIsModalVisible(true);
  
    // Validate email and password
    if (!validateEmail(email)) {
      setError("Invalid email address");
      setIsModalVisible(false);
      return;
    }
  
    if (password.length < 8) {
      setError("Password must be at least 8 characters long");
      setIsModalVisible(false);
      return;
    }
  
    setError(""); // Clear any previous error messages
  
    try {
      // Fetch device information
      const deviceName = await DeviceInfo.getDeviceName();
      const deviceOs = DeviceInfo.getSystemVersion();
      const deviceModel = DeviceInfo.getModel();
      const deviceAndroidId = await DeviceInfo.getAndroidId();
      const deviceManufacturer = await DeviceInfo.getManufacturer();
      const deviceBrand = await DeviceInfo.getBrand();
      const deviceSerialNumber = await DeviceInfo.getAndroidId();
  
      // Prepare the login payload
      const loginPayload = {
        email,
        password,
        deviceName,
        deviceOs,
        deviceModel,
        deviceAndroidId,
        deviceManufacturer,
        deviceBrand,
        deviceSerialNumber,
      };
      console.log(loginPayload);
  
      // Call the LoginApi with the updated payload
      const response = await LoginApi(loginPayload);
  
      console.log(response);
  
      if (response.data.meta.status === 200) {
        // Store the token in AsyncStorage for React Native usage
        await AsyncStorage.setItem("token", response.data.data.access_token);
  
        // Store the token in Android SharedPreferences for native usage
        await NativeModules.AndroidUtils.saveToken(response.data.data.access_token);
  
        // Start the monitoring service
        await NativeModules.MonitoringController.startService();
  
        setIsModalVisible(false);
        console.log(response.data.data);
        navigation.navigate("myTabs");
      } else {
        // Handle login failure
        setError("Login failed. Incorrect Credentials");
        setIsModalVisible(false);
      }
    } catch (error) {
      // Handle login failure
      setIsModalVisible(false);
      setError("Login failed. Please try again.");
    }
  };

  const isFormValid = email.length > 0 && password.length >= 8;

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <LoadingModal isVisible={isModalVisible} />
      <View
        style={{
          flex: 1,
          alignItems: "center",
          backgroundColor: "#496F76",
          justifyContent: "center",
        }}
      >
        <Text style={styles.titleText}>Log in to Gumshoe</Text>
        <View style={{ flex: 0.4, padding: 20, width: "100%" }}>
          <LargeInput
            placeholder="Email"
            onChangeText={(text) => setEmail(text)}
            isValid={email.length > 0}
          />
          <LargeInput
            placeholder="Password"
            secureTextEntry={true}
            onChangeText={(text) => setPassword(text)}
            isValid={password.length >= 8}
          />
          <Text style={{ color: "red", marginBottom: 10 }}>{error}</Text>
          <TouchableOpacity
            onPress={handleLogin}
            style={[styles.button, !isFormValid && { backgroundColor: "#888" }]}
            disabled={!isFormValid}
          >
            <Text style={[styles.buttonText, !isFormValid && { color: "white" }]}>Login</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => navigation.navigate("SignUp")}
          >
            <Text
              style={{ color: "white", marginTop: 10, textAlign: "center", fontFamily: "KodchasanLight" }}
            >
              Don't have an account? Sign up
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
};

export default LoginScreen;