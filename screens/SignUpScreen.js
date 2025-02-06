import React, { useState } from "react";
import {
    View,
    Text,
    TouchableOpacity,
    SafeAreaView,
    ScrollView,
    KeyboardAvoidingView,
    Platform,
    Alert,
} from "react-native";
import DeviceInfo from "react-native-device-info";
import axios from "axios";
import LargeInput from "../components/Input/LargeInput";
import LoadingModal from "../components/Modal/LoadingModal"; // Import Loading Modal

const SignUpScreen = ({ navigation }) => {
    const [firstname, setFirstname] = useState("");
    const [lastname, setLastname] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [password, setPassword] = useState("");
    const [address, setAddress] = useState("");
    const [city, setCity] = useState("");
    const [policyId, setPolicyId] = useState("");
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [error, setError] = useState("");

    // Email validation function
    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    // Form validation check
    const isFormValid = firstname && lastname && validateEmail(email) && phoneNumber && password.length >= 8 && address && city && policyId;

    const handleSignUp = async () => {
        if (!isFormValid) {
            setError("Please fill all fields correctly.");
            return;
        }

        setIsModalVisible(true);
        setError("");

        try {
            const deviceInfo = {
                deviceName: await DeviceInfo.getDeviceName(),
                deviceOs: DeviceInfo.getSystemVersion(),
                deviceModel: DeviceInfo.getModel(),
                deviceAndroidId: await DeviceInfo.getAndroidId(),
                deviceManufacturer: await DeviceInfo.getManufacturer(),
                deviceBrand: await DeviceInfo.getBrand(),
                deviceSerialNumber: await DeviceInfo.getAndroidId(),
            };

            const signUpData = {
                firstname,
                lastname,
                email,
                phoneNumber,
                password,
                address,
                city,
                policyId,
                ...deviceInfo,
            };

            console.log(signUpData)

            const response = await axios.post("https://api.blackboxservice.monster/v2/gumshoe/sign.up", signUpData);
            console.log(response)

            if (response.data.success) {
                setIsModalVisible(false);
                navigation.navigate("login");
            } else {
                setError(response.data.message || "Signup failed. Please try again.");
                setIsModalVisible(false);
            }
        } catch (error) {
            // console.error("Error during signup:", error);
            // console.log(error.response.data.message)
            setError(error.response.data.message);
            setIsModalVisible(false);
        }
    };

    return (
        <SafeAreaView style={{ flex: 1, backgroundColor: "#496F76" }}>
            <LoadingModal isVisible={isModalVisible} />
            <KeyboardAvoidingView
                style={{ flex: 1 }}
                behavior={Platform.OS === "ios" ? "padding" : "height"}
                keyboardVerticalOffset={Platform.OS === "ios" ? 64 : 0}
            >
                <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
                    <Text style={{
                        color: 'white',
                        fontSize: 23,
                        marginHorizontal: 25,
                        marginVertical: 35,
                        marginBottom: 55,
                    }}>
                        Sign Up to Gumshoe
                    </Text>

                    <View style={{ paddingHorizontal: 20 }}>
                        <LargeInput placeholder="Enter your firstname" onChangeText={setFirstname} />
                        <LargeInput placeholder="Enter your lastname" onChangeText={setLastname} />
                        <LargeInput placeholder="Enter your email" onChangeText={setEmail} />
                        <LargeInput placeholder="Enter your phone number" onChangeText={setPhoneNumber} />
                        <LargeInput placeholder="Enter your password" secureTextEntry onChangeText={setPassword} />
                        <LargeInput placeholder="Enter your address" onChangeText={setAddress} />
                        <LargeInput placeholder="Enter your city" onChangeText={setCity} />
                        <LargeInput placeholder="Enter your policy ID" onChangeText={setPolicyId} />

                        {error ? <Text style={{ color: "red", marginBottom: 10 }}>{error}</Text> : null}

                        <TouchableOpacity
                            style={{
                                backgroundColor: isFormValid ? "#EEE8E0" : "#888",
                                padding: 12,
                                borderRadius: 5,
                                alignItems: "center",
                                marginVertical: 10,
                            }}
                            onPress={handleSignUp}
                            disabled={!isFormValid}
                        >
                            <Text style={{
                                color: isFormValid ? "#E9B962" : "white",
                                fontSize: 18,
                            }}>
                                Proceed
                            </Text>
                        </TouchableOpacity>
                    </View>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

export default SignUpScreen;
