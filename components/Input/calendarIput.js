import React, { useState } from 'react';
import { View, TouchableOpacity, Text, Platform } from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { calendarInput } from '../../themes/loginStyle';

const CalendarInput = ({ placeholder, onDateChange }) => {
  const [date, setDate] = useState(new Date());
  const [showPicker, setShowPicker] = useState(false);

  const onChange = (event, selectedDate) => {
    const currentDate = selectedDate || date;
    setShowPicker(Platform.OS === 'ios'); // Keep the picker open on iOS
    setDate(currentDate);
    onDateChange(currentDate); // Pass the selected date to the parent component
  };

  return (
    <View style={{ marginBottom: 10 }}>
      <TouchableOpacity
        onPress={() => setShowPicker(true)}
        style={[calendarInput, { justifyContent: 'center' }]}
      >
        <Text style={{ color: 'white' }}>
          {date ? date.toDateString() : placeholder}
        </Text>
      </TouchableOpacity>

      {showPicker && (
        <DateTimePicker
          value={date}
          mode="date"
          display={Platform.OS === 'ios' ? 'spinner' : 'default'}
          onChange={onChange}
          style={{ backgroundColor: 'white' }} // Customize as needed
        />
      )}
    </View>
  );
};

export default CalendarInput;
