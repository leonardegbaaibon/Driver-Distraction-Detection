import React, { useRef, useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  Dimensions,
  Animated,
  TouchableOpacity,
  Alert,
  DeviceEventEmitter,
  NativeModules,
  Image,
  NativeEventEmitter,
} from 'react-native';
import { check, request, PERMISSIONS, RESULTS } from 'react-native-permissions';

const {
  MyNativeModule,
  CalendarModule,
  UltrasonicModule,
  RecordingModule,
  MotionSensorModule,
  ActivityMonitorModule,
  CameraModule,
  InteractionMonitorModule
} = NativeModules;

const { width } = Dimensions.get('window');

const App = () => {
  const [motionStatus, setMotionStatus] = useState('');
  const [isDetecting, setIsDetecting] = useState(false);
  const [appActivity, setAppActivity] = useState('No activity detected');
  const [imager, setImage] = useState('');
  const [tapCount, setTapCount] = useState(0);

  const scrollX = useRef(new Animated.Value(0)).current;
  const scrollViewRef = useRef(null);

  const screens = [
    { text: 'Faults Monitor', description: 'Track of faults in real-time.' },
    { text: 'Trips Data', description: 'Analyze your driving trips easily.' },
    { text: 'Driver Badge', description: 'Earn badges for safe driving.' },
  ];

  const backgroundColor = scrollX.interpolate({
    inputRange: [0, width, 2 * width],
    outputRange: ['#EEE8E0', '#E9B962', '#496F76'],
    extrapolate: 'clamp',
  });

  // const captureImage = async () => {
  //   try {
  //     const base64Image = await CameraModule.captureImage();
  //     // Use the base64Image
  //     console.log(base64Image)
  //     setImage(base64Image)
  //   } catch (error) {
  //     console.error(error);
  //   }
  // };


  // useEffect(() => {
  //   // Start listening for the tap count event from native code
  //   const eventListener = DeviceEventEmitter.addListener('TapCountEvent', (count) => {
  //     // Update the tap count whenever the event is received
  //     setTapCount(count);
  //     console.log(`Tap count updated: ${count}`);
  //   });

  //   // Start monitoring
  //   InteractionMonitorModule.startMonitoring();

  //   // Cleanup the event listener on component unmount
  //   return () => {
  //     eventListener.remove();
  //   };
  // }, []);


  // useEffect(() => {
  //   captureImage()
  //   const requestMicrophonePermission = async () => {
  //     const result = await request(PERMISSIONS.ANDROID.RECORD_AUDIO);
  //     if (result === RESULTS.GRANTED) {
  //       console.log('Microphone permission granted');
  //     } else {
  //       Alert.alert('Microphone permission denied');
  //     }
  //   };

  //   requestMicrophonePermission();
  //   // Initialize event listeners for activity monitoring
  //   const activitySubscription = DeviceEventEmitter.addListener(
  //     'onAppOpened',
  //     event => {
  //       if (event && event.foregroundApp) {
  //         console.log(`Foreground app detected: ${event.foregroundApp}`);
  //         setAppActivity(`Current activity: ${event.foregroundApp}`);
  //       }
  //     },
  //   );



  //   // Start monitoring app activity
  //   ActivityMonitorModule.checkAndRequestUsageStatsPermission()
  //     .then(granted => {
  //       if (granted) {
  //         ActivityMonitorModule.startActivityMonitoring();
  //       } else {
  //         console.warn('Permission not granted. Please enable it in settings.');
  //       }
  //     })
  //     .catch(error =>
  //       console.error('Error checking usage stats permission:', error),
  //     );

  //   // Clean up the listener and stop monitoring on unmount
  //   return () => {
  //     ActivityMonitorModule.stopActivityMonitoring();
  //     activitySubscription.remove();
  //   };
  // }, []);

  // const startDetection = () => {
  //   setIsDetecting(true);
  //   MotionSensorModule.startSensorDetection()
  //     .then(result => {
  //       setMotionStatus(result);
  //     })
  //     .catch(error => {
  //       console.error(error);
  //     });
  // };

  // const stopDetection = () => {
  //   setIsDetecting(false);
  //   MotionSensorModule.stopSensorDetection();
  // };

  // useEffect(() => {
  //   return () => {
  //     if (isDetecting) {
  //       stopDetection();
  //     }
  //   };
  // }, [isDetecting]);

  const handleContinue = () => {
    const currentIndex = Math.floor(scrollX._value / width);
    if (currentIndex === screens.length - 1) {
      console.log('Reached the last screen, navigating to Login');
      // Implement navigation logic here
    } else {
      scrollViewRef.current.scrollTo({
        x: scrollX._value + width,
        animated: true,
      });
    }
  };

  const playUltrasonicSound = async () => {
    try {
      const result = await UltrasonicModule.generateUltrasonic(20100, 5);
      console.log('Ultrasonic sound generated:', result);
    } catch (error) {
      console.error('Error generating ultrasonic sound:', error);
    }
  };

  const startRecording = async () => {
    try {
      const result = await RecordingModule.startRecording(5);
      console.log('Recording Result:', result);
      const { message, material, frequency, amplitude } = result;
      console.log(`Message: ${message}`);
      console.log(`Detected Material: ${material}`);
      console.log(`Detected Frequency: ${frequency}`);
      console.log(`Average Amplitude: ${amplitude}`);
    } catch (error) {
      console.error('Error during recording:', error);
    }
  };
  const handleSkip = async () => {
    try {
      await Promise.all([startRecording(), playUltrasonicSound()]);
      console.log('Recording and ultrasonic sound started simultaneously.');
    } catch (error) {
      console.error('Error starting recording and ultrasonic sound:', error);
    }
  };
  return (
    <Animated.View style={{ flex: 1, backgroundColor }}>
      <View
        style={{
          position: 'absolute',
          top: 50,
          width: '100%',
          alignItems: 'center',
        }}>
        <View
          style={{
            flexDirection: 'row',
            justifyContent: 'center',
            alignItems: 'center',
          }}>
          {screens.map((_, i) => {
            const scale = scrollX.interpolate({
              inputRange: [(i - 1) * width, i * width, (i + 1) * width],
              outputRange: [1, 1.5, 1],
              extrapolate: 'clamp',
            });

            return (
              <Animated.View
                key={i}
                style={{
                  width: 30,
                  height: 6,
                  backgroundColor: scrollX.interpolate({
                    inputRange: [(i - 1) * width, i * width, (i + 1) * width],
                    outputRange: ['#C1D6D9', '#FFFFFF', '#C1D6D9'],
                    extrapolate: 'clamp',
                  }),
                  marginHorizontal: 4,
                  borderRadius: 3,
                  transform: [{ scale }],
                }}
              />
            );
          })}
        </View>
      </View>

      <ScrollView
        ref={scrollViewRef}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onScroll={Animated.event(
          [{ nativeEvent: { contentOffset: { x: scrollX } } }],
          { useNativeDriver: false },
        )}
        scrollEventThrottle={16}>
        {screens.map((screen, index) => (
          <TouchableOpacity
            key={index}
            style={{
              width,
              justifyContent: 'center',
              alignItems: 'center',
              paddingHorizontal: 20,
              flex: 1,
            }}
            onPress={() => console.log(`Screen ${index + 1} tapped!`)}>
            <Animated.View
              style={{
                width: 200,
                height: 200,
                borderRadius: 20,
                justifyContent: 'center',
                alignItems: 'center',
                transform: [
                  {
                    scale: scrollX.interpolate({
                      inputRange: [
                        (index - 1) * width,
                        index * width,
                        (index + 1) * width,
                      ],
                      outputRange: [0.8, 1.2, 0.8],
                      extrapolate: 'clamp',
                    }),
                  },
                ],
              }}>
              {imager && (
                <Image
                  source={{ uri: `data:image/jpeg;base64,${imager}` }} // Set the image source
                  style={{
                    width: 300, // Adjust the width and height as needed
                    height: 300,
                    marginTop: 20,
                  }}
                />
              )}

              <Text style={{ fontSize: 22, fontFamily: 'JuliusSansOneRegular' }}>
                {screen.text}
              </Text>
              <Text
                style={{
                  marginTop: 10,
                  textAlign: 'center',
                  paddingHorizontal: 20,
                  fontFamily: 'JuliusSansOneRegular',
                }}>
                {screen.description}
              </Text>
              <View>
                <Text>Tap Count: {tapCount}</Text>
              </View>
            </Animated.View>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <View
        style={{
          position: 'absolute',
          bottom: 50,
          left: 0,
          right: 0,
          alignItems: 'center',
        }}>
        <TouchableOpacity
          onPress={handleContinue}
          style={{
            backgroundColor: '#FFFFFF',
            width: '90%',
            paddingVertical: 16,
            borderRadius: 10,
            marginBottom: 10,
          }}>
          <Text
            style={{
              color: '#496F76',
              textAlign: 'center',
              fontFamily: 'KodchasanLight',
              fontSize: 20,
            }}>
            Continue
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={handleSkip}
          style={{
            backgroundColor: 'transparent',
            width: '90%',
            paddingVertical: 16,
            borderRadius: 10,
            borderColor: '#FFFFFF',
            borderWidth: 2,
          }}>
          <Text
            style={{
              color: '#FFFFFF',
              textAlign: 'center',
              fontFamily: 'KodchasanLight',
              fontSize: 20,
            }}>
            Skip
          </Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );
};

export default App;
