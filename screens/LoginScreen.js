import React, { useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
} from "react-native";
import { button, buttonText, titleText } from "../themes/loginStyle";
import LargeInput from "../components/Input/LargeInput";
import { LoginApi } from "../utils/api/apis/LoginApi";
import AsyncStorage from "@react-native-async-storage/async-storage";
import LoadingModal from "../components/Modal/LoadingModal";

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
    if (!validateEmail(email)) {
      setError("Invalid email address");
      setIsModalVisible(false); // Hide the loading modal
      return;
    }

    if (password.length < 8) {
      setError("Password must be at least 8 characters long");
      setIsModalVisible(false); // Hide the loading modal
      return;
    }

    setError(""); // Clear any previous error messages

    try {
      const response = await LoginApi({ email: email, password: password });

      if (response.data.meta.status === 200) {
        // Assuming the response contains a status field indicating success
        // Store the token and navigate to the next screen upon successful login
        await AsyncStorage.setItem('token', response.data.data.access_token);
        setIsModalVisible(false);
        console.log(response.data.data)
        navigation.navigate("myTabs");
      } else {
        // Handle login failure
        setError("Login failed. Incorrect Credentials");
        setIsModalVisible(false); // Hide the loading modal
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
        <Text style={{
          color: 'white',
          fontSize: 23,
          marginHorizontal: 25,
          fontFamily: "JuliusSansOneRegular",
        }}>Log in to Gumshoe</Text>
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
            style={[button, !isFormValid && { backgroundColor: "#888" }]}
            disabled={!isFormValid}
          >
            <Text style={[buttonText, !isFormValid && { color: "white" }]}>Login</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => navigation.navigate("BlackboxCoveragescreen")}
          >
            <Text
              style={{ color: "white", marginTop: 10, textAlign: "center", fontFamily: "KodchasanLight", }}
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
