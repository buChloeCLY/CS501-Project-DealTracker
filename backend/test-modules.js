// Test script to check module exports
console.log('Testing platformService module...\n');

try {
    const platformService = require('./services/platformService');

    console.log('Module loaded successfully!');
    console.log('Exported functions:', Object.keys(platformService));
    console.log('\nFunction types:');
    console.log('  fetchFromAmazon:', typeof platformService.fetchFromAmazon);
    console.log('  fetchFromEbay:', typeof platformService.fetchFromEbay);
    console.log('  fetchFromWalmart:', typeof platformService.fetchFromWalmart);
    console.log('  transformAmazonProduct:', typeof platformService.transformAmazonProduct);
    console.log('  findBestEbayMatch:', typeof platformService.findBestEbayMatch);
    console.log('  findBestWalmartMatch:', typeof platformService.findBestWalmartMatch);

} catch (error) {
    console.error('Error loading module:', error.message);
    console.error('\nStack trace:', error.stack);
}

console.log('\n---\n');
console.log('Testing importService module...\n');

try {
    const importService = require('./services/importService');

    console.log('Module loaded successfully');
    console.log('Exported functions:', Object.keys(importService));
    console.log('\nFunction types:');
    console.log('  importInitialProducts:', typeof importService.importInitialProducts);

} catch (error) {
    console.error('Error loading module:', error.message);
    console.error('\nStack trace:', error.stack);
}