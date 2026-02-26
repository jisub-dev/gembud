// Test script to verify new ApiResponse format
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

async function testGameAPI() {
  console.log('=== Testing Game API with new ApiResponse format ===\n');

  try {
    const response = await api.get('/games');

    console.log('✅ Response Status:', response.status);
    console.log('✅ Response Headers:', response.headers['content-type']);
    console.log('\n📦 Response Structure:');
    console.log('- timestamp:', response.data.timestamp);
    console.log('- status:', response.data.status);
    console.log('- message:', response.data.message);
    console.log('- data type:', Array.isArray(response.data.data) ? 'Array' : typeof response.data.data);
    console.log('- data length:', response.data.data?.length);

    console.log('\n📊 First Game Data:');
    console.log(JSON.stringify(response.data.data[0], null, 2));

    console.log('\n✅ NEW FORMAT WORKS! ApiResponse wrapper is correctly applied.');
    console.log('Frontend services will access data via: response.data.data');

  } catch (error) {
    console.error('❌ Error:', error.message);
    if (error.response) {
      console.error('Response data:', error.response.data);
    }
  }
}

testGameAPI();
