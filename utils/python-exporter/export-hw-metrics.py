import json
import pandas as pd

def process_data(input_file, output_file):
    # Load data from the input file
    with open(input_file, 'r') as file:
        data = json.load(file)
    
    # Extract the 'values' part of the data
    values = data['data']['result'][0]['values']
    
    # Prepare the data for DataFrame
    timestamps = [item[0] for item in values]
    percentages = [item[1] for item in values]
    
    # Create a DataFrame
    df = pd.DataFrame({
        'Timestamp': timestamps,
        'Usage (%)': percentages
    })
    
    # Convert timestamps to readable date format if needed
    # df['Timestamp'] = pd.to_datetime(df['Timestamp'], unit='s')
    
    # Export to Excel
    df.to_excel(output_file, index=False)
    
    print(f'Data successfully exported to {output_file}')

# Example usage
input_file = 'input.json'  # Update this path to your actual JSON file location
output_file = 'output.xlsx'

process_data(input_file, output_file)