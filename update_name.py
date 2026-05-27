print("~~+a|b for adding, name for search~~")
while True:
    print("~"*20)
    enter = input("search/add: ")
    if enter[0] + '+' and '|' in enter:
        try:
            with open("name_change_data.txt","a") as f:
                f.write(enter.strip("+")+"\n")
            print('\nsuccess, saved new translation\n')
        except:
            print('\nfailed to write to file\n')
    else:
        key = []
        value = []
        try:
            with open("name_change_data.txt","r") as f:
                while True:
                    temp = f.readline().strip().split("|")
                    if len(temp) <= 1:
                        break
                    key.append(temp[0])
                    value.append(temp[1])
        except:
            print('\nfailed to write to file\n')
            continue
        
        printed = False
        print("\nresults:")
        for i in range(len(key)):
            if enter.lower() in key[i].lower() or key[i].lower() in enter.lower():
                print(f'\n {key[i]:<40s} | {value[i]}')
                printed = True
        if not printed:
            print("nothing found :(")
        print()

        

        