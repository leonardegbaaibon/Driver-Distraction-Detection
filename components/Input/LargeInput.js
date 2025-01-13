import React, { useState } from 'react';
import { TextInput, View, Text } from 'react-native';
import { textInput } from '../../themes/loginStyle';

const LargeInput = ({
  placeholder,
  onChangeText,
  secureTextEntry,
  isValid,
  validationMessage,
  value,
  isSubmitted // New prop to control border color and validation message
}) => {
  const [isFocused, setIsFocused] = useState(false);

  const handleChangeText = (text) => {
    onChangeText(text);
  };

  const handleBlur = () => {
    setIsFocused(false);
  };

  const handleFocus = () => {
    setIsFocused(true);
  };

  // Determine border color based on input state
  const borderColor = isFocused
    ? 'white'
    : isSubmitted
    ? isValid
      ? ''
      : 'red'
    : '#292E41';

  return (
    <View>
      <TextInput
        style={[
          textInput,
          {
            backgroundColor: 'rgba(0, 0, 0, 0.32)',
            color: 'white',
            borderColor: borderColor,
            borderWidth: 1,
            borderRadius: 5,
            fontFamily: "KodchasanLight",
            opacity:3
          },
        ]}
        placeholder={placeholder}
        onChangeText={handleChangeText}
        secureTextEntry={secureTextEntry}
        placeholderTextColor="grey"
        onBlur={handleBlur}
        onFocus={handleFocus}
        value={value}
      />
      {isSubmitted && !isValid && validationMessage ? (
        <Text style={{ color: 'red', marginTop: 5,fontFamily: "KodchasanLight", }}>{validationMessage}</Text>
      ) : null}
    </View>
  );
};

export default LargeInput;
