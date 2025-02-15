import React, { useState } from "react";
import {
    View,
    Text,
    TouchableOpacity,
    SafeAreaView,
    ScrollView,
    KeyboardAvoidingView,
    Platform,
    StyleSheet
} from "react-native";
import axios from "axios";
import LargeInput from "../components/Input/LargeInput";
import LoadingModal from "../components/Modal/LoadingModal";
import Icon from "react-native-vector-icons/MaterialIcons";
import AsyncStorage from "@react-native-async-storage/async-storage";


const VehicleScreen = ({ navigation }) => {
    const [vehicleModel, setVehicleModel] = useState("");
    const [vehicleMake, setVehicleMake] = useState("");
    const [vehicleYear, setVehicleYear] = useState("");
    const [vehicleVin, setVehicleVin] = useState("");
    const [vehicleColor, setVehicleColor] = useState("");
    const [vehicleRegistrationNumber, setVehicleRegistrationNumber] = useState("");
    const [engineNumber, setEngineNumber] = useState("");
    const [name, setName] = useState("");

    const [isModalVisible, setIsModalVisible] = useState(false);
    const [error, setError] = useState("");

    // Form validation: ensure all fields are filled and vehicleYear is a valid number.
    const isFormValid =
        vehicleModel.trim() &&
        vehicleMake.trim() &&
        vehicleYear.trim() &&
        !isNaN(vehicleYear) &&
        vehicleVin.trim() &&
        vehicleColor.trim() &&
        vehicleRegistrationNumber.trim() &&
        engineNumber.trim() &&
        name.trim();

    const handleSubmit = async () => {
        if (!isFormValid) {
            setError("Please fill all fields correctly.");
            return;
        }

        setIsModalVisible(true);
        setError("");

        const vehicleData = {
            vehicleModel: vehicleModel.trim(),
            vehicleMake: vehicleMake.trim(),
            vehicleYear: parseInt(vehicleYear),
            vehicleVin: vehicleVin.trim(),
            vehicleColor: vehicleColor.trim(),
            vehicleRegistrationNumber: vehicleRegistrationNumber.trim(),
            engineNumber: engineNumber.trim(),
            name: name.trim(),
        };

        try {
            const token = await AsyncStorage.getItem("token");

            const response = await axios.post(
                "https://api.blackboxservice.monster/v2/gumshoe/vehicle",
                vehicleData,
                {
                    headers: {
                        // Replace <YOUR_TOKEN> with your actual token or fetch it from your app's storage.
                        "Authorization": "Bearer <YOUR_TOKEN>",
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}` 
                    },
                    
                      
                }
            );
            console.log(response.data);

            if (response.data.success) {
                setIsModalVisible(false);
                // Navigate back or to another screen as needed.
                navigation.goBack();
            } else {
                setError(response.data.message || "Submission failed. Please try again.");
                setIsModalVisible(false);
            }
        } catch (err) {
            console.error("Error during vehicle submission:", err);
            setError(err.response?.data?.message || "An error occurred.");
            setIsModalVisible(false);
        }
    };

    return (
        <SafeAreaView style={{ flex: 1, backgroundColor: "#496F76" }}>
            <LoadingModal isVisible={isModalVisible} />

            <View style={{
                flexDirection: "row",
                alignItems: "center",
                justifyContent: "space-between",
                padding: 16,
                borderBottomWidth: 1,
                borderBottomColor: "#496F76",
            }}>
                <TouchableOpacity
                    onPress={() => navigation.goBack()}
                    style={{
                        marginRight: 10,
                    }}
                >
                    <Icon name="arrow-back" size={25} color="white" />
                </TouchableOpacity>
                <Text style={{
                    color: "white",
                    fontSize: 20,
                    // fontFamily: "KodchasanLight",
                }}></Text>
                <TouchableOpacity >
                    <Icon name="settings" size={25} color="transparent" />
                </TouchableOpacity>
            </View>
            <KeyboardAvoidingView
                style={{ flex: 1 }}
                behavior={Platform.OS === "ios" ? "padding" : "height"}
                keyboardVerticalOffset={Platform.OS === "ios" ? 64 : 0}
            >
                <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
                    <Text style={{
                        color: "white",
                        fontSize: 23,
                        marginHorizontal: 25,
                        marginVertical: 35,
                        marginBottom: 55,
                    }}>
                        Enter Vehicle Details
                    </Text>
                    <View style={{ paddingHorizontal: 20 }}>
                        <LargeInput
                            placeholder="Enter vehicle model"
                            onChangeText={setVehicleModel}
                            value={vehicleModel}
                        />
                        <LargeInput
                            placeholder="Enter vehicle make"
                            onChangeText={setVehicleMake}
                            value={vehicleMake}
                        />
                        <LargeInput
                            placeholder="Enter vehicle year"
                            onChangeText={setVehicleYear}
                            value={vehicleYear}
                            keyboardType="numeric"
                        />
                        <LargeInput
                            placeholder="Enter vehicle VIN"
                            onChangeText={setVehicleVin}
                            value={vehicleVin}
                        />
                        <LargeInput
                            placeholder="Enter vehicle color"
                            onChangeText={setVehicleColor}
                            value={vehicleColor}
                        />
                        <LargeInput
                            placeholder="Enter registration number"
                            onChangeText={setVehicleRegistrationNumber}
                            value={vehicleRegistrationNumber}
                        />
                        <LargeInput
                            placeholder="Enter engine number"
                            onChangeText={setEngineNumber}
                            value={engineNumber}
                        />
                        <LargeInput
                            placeholder="Enter your name"
                            onChangeText={setName}
                            value={name}
                        />
                        {error ? (
                            <Text style={{ color: "red", marginBottom: 10 }}>{error}</Text>
                        ) : null}
                        <TouchableOpacity
                            style={{
                                backgroundColor: isFormValid ? "#EEE8E0" : "#888",
                                padding: 12,
                                borderRadius: 5,
                                alignItems: "center",
                                marginVertical: 10,
                            }}
                            onPress={handleSubmit}
                            disabled={!isFormValid}
                        >
                            <Text style={{
                                color: isFormValid ? "#E9B962" : "white",
                                fontSize: 18,
                            }}>
                                Submit Vehicle
                            </Text>
                        </TouchableOpacity>
                    </View>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

export default VehicleScreen;
